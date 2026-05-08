package team23.q_check.common.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "frontend")
public record FrontendProperties(
        @NotBlank String authCallbackUrl
) {}
