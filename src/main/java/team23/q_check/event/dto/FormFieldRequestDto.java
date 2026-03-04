package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "행사 폼 필드 생성 요청 DTO")
public record FormFieldRequestDto(
        @Schema(example = "SELECT")
        String type,
        @Schema(example = "식사 메뉴")
        String label,
        @Schema(example = "true")
        Boolean required,
        @Schema(example = "[\"한식\",\"양식\",\"샐러드\"]")
        List<String> options
) {
}
