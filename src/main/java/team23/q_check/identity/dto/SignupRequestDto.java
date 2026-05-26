package team23.q_check.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 요청 DTO")
public record SignupRequestDto(
        @Schema(example = "김지윤", description = "이름")
        String name,
        @Schema(example = "qcheck_user", description = "아이디 (중복 불가)")
        String username,
        @Schema(example = "user@example.com", description = "이메일 (가입용 JWT의 이메일과 일치해야 함)")
        String email
) {}
