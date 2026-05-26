package team23.q_check.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import team23.q_check.club.domain.model.ClubRole;

@Schema(description = "클럽 상세 응답 DTO")
public record ClubDetailResponseDto(
        @Schema(example = "1")
        Long clubId,
        @Schema(example = "UMC")
        String clubName,
        @Schema(example = "University MakeUs Challenge club")
        String clubDescription,
        @Schema(example = "12")
        Long memberCount,
        @Schema(example = "OWNER")
        ClubRole myRole,
        @Schema(description = "디스코드 서버 ID", example = "123456789012345678")
        String discordGuildId
) {
}
