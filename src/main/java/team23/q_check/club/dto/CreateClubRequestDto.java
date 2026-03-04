package team23.q_check.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "클럽 생성 요청 DTO")
public record CreateClubRequestDto(
        @Schema(example = "UMC")
        String name,
        @Schema(example = "University MakeUs Challenge club")
        String description,
        @Schema(example = "123456789012345678")
        String discordGuildId,
        @Schema(example = "https://example.com/club-cover.png")
        String coverImageUrl
) {
}
