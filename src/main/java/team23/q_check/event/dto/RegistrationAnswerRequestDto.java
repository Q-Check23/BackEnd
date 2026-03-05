package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "참가 신청 답변 요청 DTO")
public record RegistrationAnswerRequestDto(
        @Schema(example = "11")
        Long fieldId,
        @Schema(example = "한식")
        String value
) {
}
