package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "참가 신청 생성 요청 DTO")
public record CreateRegistrationRequestDto(
        List<RegistrationAnswerRequestDto> answers
) {
}
