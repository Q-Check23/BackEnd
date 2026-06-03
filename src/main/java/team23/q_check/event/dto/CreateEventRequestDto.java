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
        @Schema(description = "디스코드 채널 생성 여부 (true면 채널 생성, false/null이면 생성 안 함)", example = "true")
        Boolean createDiscordChannel,
        @Schema(description = "기존 디스코드 채널 ID (createDiscordChannel=true이고 이 값이 있으면 새 채널을 생성하지 않고 이 채널 사용)",
                example = "1234567890")
        String discordChannelId,
        @Schema(description = "참가비(원)", example = "10000")
        BigDecimal registerFee,
        @Schema(description = "사전등록 시 참가자 정보(폼) 수집 여부. null/true면 폼 수집, false면 폼 없이 원클릭 등록. 생성 시에만 지정 가능 (수정 불가)",
                example = "true")
        Boolean collectRegistrationInfo,
        List<FormFieldRequestDto> formFields
) {
}
