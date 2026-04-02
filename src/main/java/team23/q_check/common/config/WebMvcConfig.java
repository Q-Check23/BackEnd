package team23.q_check.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import team23.q_check.common.auth.CurrentUserIdArgumentResolver;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    public WebMvcConfig(CurrentUserIdArgumentResolver currentUserIdArgumentResolver) {
        this.currentUserIdArgumentResolver = currentUserIdArgumentResolver;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserIdArgumentResolver);
    }
}
