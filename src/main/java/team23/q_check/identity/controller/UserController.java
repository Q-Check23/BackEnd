package team23.q_check.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team23.q_check.common.auth.CurrentUserId;
import team23.q_check.common.response.ApiResponse;
import team23.q_check.identity.dto.MyUserResponseDto;
import team23.q_check.identity.dto.UpdateMyUserRequestDto;
import team23.q_check.identity.dto.UserSearchResultDto;
import team23.q_check.identity.domain.service.UserService;

import java.util.List;

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
    public ApiResponse<MyUserResponseDto> getMyUser(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId
    ) {
        return ApiResponse.ok(userService.getMyUser(currentUserId));
    }

    @Operation(summary = "내 정보 수정")
    @PutMapping("/me")
    public ApiResponse<MyUserResponseDto> updateMyUser(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @RequestBody UpdateMyUserRequestDto request
    ) {
        return ApiResponse.ok(userService.updateMyUser(currentUserId, request));
    }

    @Operation(summary = "회원 탈퇴 (soft-delete)",
            description = "deleted_at을 현재 시각으로 표시하고 refresh token을 무효화합니다. " +
                    "이후 모든 user 조회는 이 사용자를 반환하지 않습니다.")
    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMyUser(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId
    ) {
        userService.deleteMyUser(currentUserId);
        return ApiResponse.ok(null);
    }

    @Operation(
            summary = "사용자 검색",
            description = "닉네임(부분일치) 또는 이메일(정확매치)로 사용자를 검색합니다. " +
                    "둘 중 하나는 반드시 입력해야 하며, 결과에는 이메일이 포함되지 않습니다. " +
                    "닉네임 검색은 최대 20명까지 반환합니다."
    )
    @GetMapping("/search")
    public ApiResponse<List<UserSearchResultDto>> searchUsers(
            @Parameter(description = "닉네임 부분일치 (대소문자 무시)") @RequestParam(required = false) String nickname,
            @Parameter(description = "이메일 정확매치") @RequestParam(required = false) String email
    ) {
        return ApiResponse.ok(userService.searchUsers(nickname, email));
    }
}
