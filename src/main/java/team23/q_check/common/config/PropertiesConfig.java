package team23.q_check.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({DiscordProperties.class, JwtProperties.class, FrontendProperties.class})
public class PropertiesConfig {}
