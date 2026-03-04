package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "참가 신청 생성 응답 DTO")
public record CreateRegistrationResponseDto(
        @Schema(example = "200")
        Long registrationId,
        @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
        String qrToken
) {
}
