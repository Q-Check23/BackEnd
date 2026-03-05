package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "행사 수정 요청 DTO")
public record UpdateEventRequestDto(
        @Schema(example = "2026 상반기 OT - 장소 변경")
        String title,
        @Schema(example = "2026-03-10T20:00:00")
        String startTime,
        @Schema(example = "서울 강남구 스타트업 캠퍼스")
        String location,
        @Schema(example = "true")
        Boolean isActive
) {
}
