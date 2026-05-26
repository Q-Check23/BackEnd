package team23.q_check.integrations.domain.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import team23.q_check.common.config.DiscordProperties;

import java.util.Map;

@Service
public class DiscordBotService {

    private static final Logger log = LoggerFactory.getLogger(DiscordBotService.class);
    private static final String GUILDS_CHANNELS_URL = "https://discord.com/api/v10/guilds/{guildId}/channels";

    private final RestClient restClient;
    private final DiscordProperties discordProps;

    public DiscordBotService(DiscordProperties discordProps) {
        this.discordProps = discordProps;
        this.restClient = RestClient.create();
    }

    /**
     * 지정된 길드에 텍스트 채널을 생성하고 채널 ID를 반환한다.
     * botToken이 설정되지 않았거나 API 호출이 실패하면 null을 반환한다.
     */
    public String createTextChannel(String guildId, String channelName) {
        if (discordProps.botToken() == null || discordProps.botToken().isBlank()) {
            log.debug("discord.bot_token is not configured; skipping channel creation");
            return null;
        }

        long startNanos = System.nanoTime();
        try {
            ChannelResponse response = restClient.post()
                    .uri(GUILDS_CHANNELS_URL, guildId)
                    .header("Authorization", "Bot " + discordProps.botToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("name", channelName, "type", 0))
                    .retrieve()
                    .body(ChannelResponse.class);

            String channelId = response != null ? response.id() : null;
            log.info("discord.create_channel status=200 guildId={} channelId={} latencyMs={}",
                    guildId, channelId, elapsedMs(startNanos));
            return channelId;
        } catch (RestClientException e) {
            log.warn("discord.create_channel status=failed guildId={} latencyMs={} msg={}",
                    guildId, elapsedMs(startNanos), e.getMessage());
            return null;
        }
    }

    public boolean isBotConfigured() {
        return discordProps.botToken() != null && !discordProps.botToken().isBlank();
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private record ChannelResponse(
            @JsonProperty("id") String id
    ) {}
}
