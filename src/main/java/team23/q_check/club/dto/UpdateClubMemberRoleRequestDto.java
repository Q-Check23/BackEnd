package team23.q_check.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "클럽 멤버 역할 변경 요청 DTO")
public record UpdateClubMemberRoleRequestDto(
        @Schema(example = "ADMIN")
        String role
) {
}
