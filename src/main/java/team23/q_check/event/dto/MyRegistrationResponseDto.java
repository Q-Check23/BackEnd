package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "내 참가 신청 조회 응답 DTO")
public record MyRegistrationResponseDto(
        @Schema(example = "200")
        Long registrationId,
        @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
        String qrToken,
        @Schema(example = "REGISTERED")
        String status,
        @Schema(example = "2026-03-10T19:10:00")
        String createdAt,
        List<RegistrationAnswerResponseDto> answers
) {
}
