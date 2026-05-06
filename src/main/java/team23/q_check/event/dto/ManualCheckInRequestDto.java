package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "수동 체크인 요청 DTO")
public record ManualCheckInRequestDto(
        @Schema(example = "200")
        Long registrationId
) {
}
