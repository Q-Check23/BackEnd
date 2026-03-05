package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "QR 체크인 요청 DTO")
public record CheckInRequestDto(
        @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
        String qrToken
) {
}
