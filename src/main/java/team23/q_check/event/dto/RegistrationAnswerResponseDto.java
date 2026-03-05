package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "참가 신청 답변 응답 DTO")
public record RegistrationAnswerResponseDto(
        @Schema(example = "11")
        Long fieldId,
        @Schema(example = "식사 메뉴")
        String label,
        @Schema(example = "한식")
        String value
) {
}
