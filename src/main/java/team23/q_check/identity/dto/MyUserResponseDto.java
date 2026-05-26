package team23.q_check.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 사용자 정보 응답 DTO")
public record MyUserResponseDto(
        @Schema(example = "1")
        Long id,
        @Schema(example = "qcheck_user")
        String username,
        @Schema(example = "김지윤")
        String realName,
        @Schema(example = "user@example.com")
        String email,
        @Schema(example = "010-1234-5678")
        String phone
) {
}
