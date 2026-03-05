package team23.q_check.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import team23.q_check.club.domain.model.ClubRole;

@Schema(description = "클럽 멤버 응답 DTO")
public record ClubMemberResponseDto(
        @Schema(example = "10")
        Long memberId,
        @Schema(example = "7")
        Long userId,
        @Schema(example = "kimjyun")
        String username,
        @Schema(example = "ADMIN")
        ClubRole role
) {
}
