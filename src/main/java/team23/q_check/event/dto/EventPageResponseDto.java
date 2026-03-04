package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "행사 목록 페이징 응답 DTO")
public record EventPageResponseDto(
        @Schema(example = "0")
        int page,
        @Schema(example = "10")
        int size,
        @Schema(example = "3")
        int totalPages,
        @Schema(example = "25")
        long totalElements,
        List<EventListItemDto> items
) {
}
