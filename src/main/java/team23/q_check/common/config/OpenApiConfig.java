package team23.q_check.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Q-Check API",
                version = "v1",
                description = "Q-Check backend API documentation"
        ),
        security = @SecurityRequirement(name = "X-USER-ID")
)
@SecurityScheme(
        name = "X-USER-ID",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-USER-ID",
        description = "Development-only authentication header for current user id"
)
public class OpenApiConfig {
}
