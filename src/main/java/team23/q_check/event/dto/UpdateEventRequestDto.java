package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "행사 수정 요청 DTO — null 필드는 변경하지 않음")
public record UpdateEventRequestDto(
        @Schema(example = "2026 상반기 OT - 장소 변경")
        String title,
        @Schema(example = "장소가 변경되었습니다")
        String description,
        @Schema(example = "2026-03-10T20:00:00")
        String startTime,
        @Schema(example = "2026-03-10T22:00:00")
        String endTime,
        @Schema(example = "서울 강남구 스타트업 캠퍼스")
        String location,
        @Schema(example = "1234567890")
        String discordChannelId,
        @Schema(example = "10000")
        BigDecimal registerFee,
        @Schema(example = "true")
        Boolean isActive
) {
}
