package team23.q_check.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "초대 코드로 클럽 가입 요청 DTO")
public record JoinClubRequestDto(
        @Schema(example = "ABCD1234")
        String inviteCode
) {
}
