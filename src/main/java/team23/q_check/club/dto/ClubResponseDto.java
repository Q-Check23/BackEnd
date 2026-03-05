package team23.q_check.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "동아리 응답 DTO")
public record ClubResponseDto(
        @Schema(example = "1")
        Long id,
        @Schema(example = "UMC")
        String name,
        @Schema(example = "University MakeUs Challenge club")
        String description
) {
}
