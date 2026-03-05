package team23.q_check.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "클럽 멤버 추가 요청 DTO")
public record AddClubMemberRequestDto(
        @Schema(example = "7")
        Long userId
) {
}
