package team23.q_check.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Q-Check API",
                version = "v1",
                description = "Q-Check backend API documentation"
        )
)
public class OpenApiConfig {
}
