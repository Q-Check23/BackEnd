package team23.q_check.common.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SwaggerUiRedirectController {

    @GetMapping("/swagger-ui")
    public String redirectToSwaggerUiHtml() {
        return "redirect:/swagger-ui.html";
    }
}
