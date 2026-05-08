package team23.q_check.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공지사항 작성 요청 DTO")
public record CreateNoticeRequestDto(
        @Schema(example = "정기 회의 공지")
        String title,
        @Schema(example = "이번 주 토요일 오후 7시...")
        String content
) {
}
