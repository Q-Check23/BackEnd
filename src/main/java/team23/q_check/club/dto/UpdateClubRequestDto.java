package team23.q_check.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "클럽 정보 수정 요청 DTO (모든 필드 선택)")
public record UpdateClubRequestDto(
        @Schema(example = "UMC")
        String name,
        @Schema(example = "University MakeUs Challenge club")
        String description,
        @Schema(example = "https://example.com/club-cover.png")
        String coverImageUrl,
        @Schema(description = "디스코드 서버 ID (빈 문자열이면 연동 해제)", example = "123456789012345678")
        String discordGuildId
) {
}
