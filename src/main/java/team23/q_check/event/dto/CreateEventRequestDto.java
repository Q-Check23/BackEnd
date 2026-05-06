package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "행사 생성 요청 DTO")
public record CreateEventRequestDto(
        @Schema(example = "1")
        Long clubId,
        @Schema(example = "2026 상반기 OT")
        String title,
        @Schema(example = "신입 부원 환영회")
        String description,
        @Schema(example = "2026-03-10T19:00:00")
        String startTime,
        @Schema(example = "2026-03-10T22:00:00")
        String endTime,
        @Schema(example = "서울 강남구 스타트업 캠퍼스")
        String location,
        @Schema(description = "행사 알림이 갈 디스코드 채널 ID", example = "1234567890")
        String discordChannelId,
        @Schema(description = "참가비(원)", example = "10000")
        BigDecimal registerFee,
        List<FormFieldRequestDto> formFields
) {
}
