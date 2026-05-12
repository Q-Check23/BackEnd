package team23.q_check.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 활동 통계 응답 DTO")
public record MyUserStatsResponseDto(
        @Schema(description = "체크인까지 완료한 누적 참여 행사 수", example = "12")
        long attendedEventCount,
        @Schema(description = "사전 등록만 한 다가오는 행사 수", example = "3")
        long upcomingEventCount
) {
}
