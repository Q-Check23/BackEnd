package team23.q_check.identity.domain.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import team23.q_check.common.config.DiscordProperties;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;

@Service
public class DiscordOAuthService {

    private static final Logger log = LoggerFactory.getLogger(DiscordOAuthService.class);

    private final RestClient restClient;
    private final DiscordProperties discordProps;

    public DiscordOAuthService(DiscordProperties discordProps) {
        this.discordProps = discordProps;
        this.restClient = RestClient.create();
    }

    public String getAuthorizationUrl(String state) {
        return getAuthorizationUrl(state, false);
    }

    /**
     * Discord OAuth 인증 URL을 생성한다.
     *
     * @param state CSRF 방지용 랜덤 state
     * @param silent true 면 prompt=none 을 붙여 이미 인가된 사용자에 대해 동의 화면을
     *               건너뛰고 즉시 redirect 한다. 인가가 안 돼 있으면 Discord 가 error
     *               로 돌려보내므로 호출자는 그 경우를 폴백(일반 흐름 재시도) 해야 한다.
     */
    public String getAuthorizationUrl(String state, boolean silent) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(discordProps.oauthUrl())
                .queryParam("client_id", discordProps.clientId())
                .queryParam("redirect_uri", discordProps.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", "identify email")
                .queryParam("state", state);
        if (silent) {
            builder.queryParam("prompt", "none");
        }
        return builder.build().toUriString();
    }

    public DiscordTokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", discordProps.clientId());
        body.add("client_secret", discordProps.clientSecret());
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", discordProps.redirectUri());

        long startNanos = System.nanoTime();
        try {
            DiscordTokenResponse response = restClient.post()
                    .uri(discordProps.tokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(DiscordTokenResponse.class);
            log.info("discord.token_exchange status=200 latencyMs={}", elapsedMs(startNanos));
            return response;
        } catch (RestClientResponseException e) {
            log.warn("discord.token_exchange status={} latencyMs={} body={}",
                    e.getStatusCode().value(), elapsedMs(startNanos), e.getResponseBodyAsString());
            throw new AppException(ErrorCode.UNAUTHORIZED, "Discord 인가코드 교환에 실패했습니다");
        } catch (RestClientException e) {
            log.warn("discord.token_exchange status=network_error latencyMs={} msg={}",
                    elapsedMs(startNanos), e.getMessage());
            throw new AppException(ErrorCode.UNAUTHORIZED, "Discord 인가코드 교환에 실패했습니다");
        }
    }

    public DiscordUserInfo getUserInfo(String discordAccessToken) {
        long startNanos = System.nanoTime();
        try {
            DiscordUserInfo info = restClient.get()
                    .uri(discordProps.userUrl())
                    .header("Authorization", "Bearer " + discordAccessToken)
                    .retrieve()
                    .body(DiscordUserInfo.class);
            log.info("discord.user_info status=200 latencyMs={} discordId={}",
                    elapsedMs(startNanos), info != null ? info.id() : null);
            return info;
        } catch (RestClientResponseException e) {
            log.warn("discord.user_info status={} latencyMs={} body={}",
                    e.getStatusCode().value(), elapsedMs(startNanos), e.getResponseBodyAsString());
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Discord 사용자 정보 조회에 실패했습니다");
        } catch (RestClientException e) {
            log.warn("discord.user_info status=network_error latencyMs={} msg={}",
                    elapsedMs(startNanos), e.getMessage());
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Discord 사용자 정보 조회에 실패했습니다");
        }
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    public record DiscordTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn,
            @JsonProperty("refresh_token") String refreshToken,
            String scope
    ) {}

    public record DiscordUserInfo(
            String id,
            String username,
            String email,
            Boolean verified
    ) {}
}
