package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이벤트 응답 DTO")
public record EventResponseDto(
        @Schema(example = "10")
        Long id,
        @Schema(example = "Spring Boot Study")
        String title,
        @Schema(example = "2026-03-05T19:00:00")
        String startTime
) {
}
