package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "행사 목록 아이템 DTO")
public record EventListItemDto(
        @Schema(example = "100")
        Long eventId,
        @Schema(example = "2026 상반기 OT")
        String title,
        @Schema(example = "2026-03-10T19:00:00")
        String startTime,
        @Schema(example = "서울 강남구 스타트업 캠퍼스")
        String location,
        @Schema(example = "true")
        Boolean isActive
) {
}
