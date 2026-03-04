package team23.q_check.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team23.q_check.common.auth.CurrentUserId;
import team23.q_check.common.response.ApiResponse;
import team23.q_check.identity.dto.MyUserResponseDto;
import team23.q_check.identity.dto.UpdateMyUserRequestDto;
import team23.q_check.identity.service.UserService;

@Tag(name = "User", description = "User API")
@SecurityRequirement(name = "X-USER-ID")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ApiResponse<MyUserResponseDto> getMyUser(@CurrentUserId Long currentUserId) {
        return ApiResponse.ok(userService.getMyUser(currentUserId));
    }

    @Operation(summary = "내 정보 수정")
    @PutMapping("/me")
    public ApiResponse<MyUserResponseDto> updateMyUser(
            @CurrentUserId Long currentUserId,
            @RequestBody UpdateMyUserRequestDto request
    ) {
        return ApiResponse.ok(userService.updateMyUser(currentUserId, request));
    }
}
