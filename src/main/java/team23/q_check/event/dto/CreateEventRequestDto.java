package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "행사 생성 요청 DTO")
public record CreateEventRequestDto(
        @Schema(example = "1")
        Long clubId,
        @Schema(example = "2026 상반기 OT")
        String title,
        @Schema(example = "2026-03-10T19:00:00")
        String startTime,
        List<FormFieldRequestDto> formFields
) {
}
