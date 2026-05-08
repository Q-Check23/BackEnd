package team23.q_check.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import team23.q_check.club.domain.model.ClubRole;

@Schema(description = "공지사항 응답 DTO")
public record NoticeResponseDto(
        @Schema(example = "1")
        Long noticeId,
        @Schema(example = "정기 회의 공지")
        String title,
        @Schema(example = "이번 주 토요일 오후 7시...")
        String content,
        @Schema(example = "홍길동")
        String authorName,
        @Schema(example = "OWNER")
        ClubRole authorRole,
        @Schema(example = "2026-05-08T12:34:56")
        String createdAt
) {
}
