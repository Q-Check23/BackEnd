package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "관리자 참가자 목록 아이템 DTO")
public record AdminRegistrationItemDto(
        @Schema(example = "200")
        Long registrationId,
        @Schema(example = "7")
        Long userId,
        @Schema(example = "kimjyun")
        String username,
        @Schema(example = "김지윤")
        String realName,
        @Schema(example = "REGISTERED")
        String status,
        @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
        String qrToken,
        @Schema(description = "체크인 시각 (미체크인 시 null)", example = "2026-05-29T14:30:00")
        LocalDateTime checkInTime,
        List<RegistrationAnswerResponseDto> answers
) {
}
