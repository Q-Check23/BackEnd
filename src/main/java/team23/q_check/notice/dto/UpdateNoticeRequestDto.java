package team23.q_check.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공지사항 수정 요청 DTO")
public record UpdateNoticeRequestDto(
        @Schema(example = "정기 회의 공지 (수정)")
        String title,
        @Schema(example = "변경된 일정으로 안내드립니다.")
        String content
) {
}
