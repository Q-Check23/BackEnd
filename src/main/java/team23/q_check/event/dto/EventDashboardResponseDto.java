package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "행사 대시보드 통계 응답 DTO")
public record EventDashboardResponseDto(
        @Schema(example = "120")
        long totalRegistrations,
        @Schema(example = "92")
        long checkedInCount,
        @Schema(example = "5", description = "취소(CANCELED) 건수")
        long canceledCount,
        @Schema(description = "체크인율 (0.0 ~ 1.0)", example = "0.766")
        double checkInRate
) {
}
