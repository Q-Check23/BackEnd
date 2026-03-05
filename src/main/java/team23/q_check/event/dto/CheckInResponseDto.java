package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "QR 체크인 응답 DTO")
public record CheckInResponseDto(
        @Schema(example = "200")
        Long registrationId,
        @Schema(example = "2026-03-10T19:20:00")
        String checkInTime,
        @Schema(example = "김지윤")
        String username
) {
}
