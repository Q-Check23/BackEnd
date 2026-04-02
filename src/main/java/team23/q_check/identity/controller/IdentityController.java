package team23.q_check.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team23.q_check.common.response.ApiResponse;
import team23.q_check.identity.dto.UserResponseDto;
import team23.q_check.identity.domain.service.UserService;

@Tag(name = "Identity", description = "Identity API")
@RestController
@RequestMapping("/api/identities")
public class IdentityController {

    private final UserService userService;

    public IdentityController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "샘플 사용자 조회")
    @GetMapping("/sample")
    public ApiResponse<UserResponseDto> getSampleUser() {
        return ApiResponse.ok(userService.getSampleUser());
    }
}
