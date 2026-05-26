package team23.q_check.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 사용자 정보 수정 요청 DTO")
public record UpdateMyUserRequestDto(
        @Schema(example = "김지윤")
        String realName,
        @Schema(example = "qcheck_user")
        String username,
        @Schema(example = "010-1234-5678")
        String phone
) {
}
