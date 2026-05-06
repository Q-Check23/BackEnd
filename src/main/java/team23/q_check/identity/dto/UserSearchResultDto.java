package team23.q_check.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 검색 결과 DTO")
public record UserSearchResultDto(
        @Schema(example = "7")
        Long userId,
        @Schema(example = "kimjyun")
        String username,
        @Schema(example = "김지윤", description = "사용자가 등록한 실명. 등록하지 않았으면 null.")
        String realName
) {
}
