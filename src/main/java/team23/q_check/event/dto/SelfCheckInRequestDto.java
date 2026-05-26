package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SelfCheckInRequestDto(
        @Schema(example = "1")
        Long eventId
) {}
