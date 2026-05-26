package team23.q_check.integrations.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team23.q_check.common.config.DiscordProperties;
import team23.q_check.common.response.ApiResponse;
import team23.q_check.integrations.domain.service.DiscordBotService;

@RestController
@RequestMapping("/api/discord")
public class DiscordBotController {

    private static final long BOT_PERMISSIONS = 268435472L; // MANAGE_CHANNELS + MANAGE_ROLES

    private final DiscordProperties discordProps;
    private final DiscordBotService discordBotService;

    public DiscordBotController(DiscordProperties discordProps, DiscordBotService discordBotService) {
        this.discordProps = discordProps;
        this.discordBotService = discordBotService;
    }

    @GetMapping("/bot-invite-url")
    public ApiResponse<String> getBotInviteUrl() {
        String url = String.format(
                "https://discord.com/oauth2/authorize?client_id=%s&scope=bot&permissions=%d",
                discordProps.clientId(),
                BOT_PERMISSIONS
        );

        return ApiResponse.ok(url);
    }
}
