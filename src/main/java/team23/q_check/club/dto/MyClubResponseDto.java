package team23.q_check.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import team23.q_check.club.domain.model.ClubRole;

@Schema(description = "내 클럽 목록 응답 DTO")
public record MyClubResponseDto(
        @Schema(example = "1")
        Long clubId,
        @Schema(example = "UMC")
        String clubName,
        @Schema(example = "University MakeUs Challenge club")
        String clubDescription,
        @Schema(example = "OWNER")
        ClubRole myRole
) {
}
