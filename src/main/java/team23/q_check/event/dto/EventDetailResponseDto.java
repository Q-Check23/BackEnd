package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "행사 상세 응답 DTO")
public record EventDetailResponseDto(
        @Schema(example = "100")
        Long eventId,
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
        @Schema(example = "1234567890")
        String discordChannelId,
        @Schema(example = "10000")
        BigDecimal registerFee,
        @Schema(example = "true")
        Boolean isActive,
        @Schema(description = "사전등록 시 참가자 정보(폼) 수집 여부. false 면 프론트는 폼 없이 원클릭 등록 UI 를 표시",
                example = "true")
        Boolean collectRegistrationInfo,
        List<FormFieldResponseDto> formFields
) {
}
