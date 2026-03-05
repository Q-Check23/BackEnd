package team23.q_check.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 응답 DTO")
public record UserResponseDto(
        @Schema(example = "7")
        Long id,
        @Schema(example = "123456789012345678")
        String discordId,
        @Schema(example = "kimjyun")
        String username
) {
}
