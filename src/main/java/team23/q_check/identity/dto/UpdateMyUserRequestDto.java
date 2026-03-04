package team23.q_check.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 사용자 정보 수정 요청 DTO")
public record UpdateMyUserRequestDto(
        @Schema(example = "김지윤")
        String realName
) {
}
