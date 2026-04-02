package team23.q_check.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discord")
public record DiscordProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String oauthUrl,
        String tokenUrl,
        String userUrl
) {}
