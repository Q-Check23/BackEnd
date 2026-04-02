package team23.q_check.identity.domain.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import team23.q_check.common.config.DiscordProperties;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;

@Service
public class DiscordOAuthService {

    private final RestClient restClient;
    private final DiscordProperties discordProps;

    public DiscordOAuthService(DiscordProperties discordProps) {
        this.discordProps = discordProps;
        this.restClient = RestClient.create();
    }

    public String getAuthorizationUrl() {
        return UriComponentsBuilder.fromUriString(discordProps.oauthUrl())
                .queryParam("client_id", discordProps.clientId())
                .queryParam("redirect_uri", discordProps.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", "identify email")
                .build()
                .toUriString();
    }

    public DiscordTokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", discordProps.clientId());
        body.add("client_secret", discordProps.clientSecret());
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", discordProps.redirectUri());

        try {
            return restClient.post()
                    .uri(discordProps.tokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(DiscordTokenResponse.class);
        } catch (RestClientException e) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Discord 인가코드 교환에 실패했습니다");
        }
    }

    public DiscordUserInfo getUserInfo(String discordAccessToken) {
        try {
            return restClient.get()
                    .uri(discordProps.userUrl())
                    .header("Authorization", "Bearer " + discordAccessToken)
                    .retrieve()
                    .body(DiscordUserInfo.class);
        } catch (RestClientException e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Discord 사용자 정보 조회에 실패했습니다");
        }
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
