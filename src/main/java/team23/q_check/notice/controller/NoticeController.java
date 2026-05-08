package team23.q_check.notice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import team23.q_check.common.auth.CurrentUserId;
import team23.q_check.common.response.ApiResponse;
import team23.q_check.notice.domain.service.NoticeService;
import team23.q_check.notice.dto.CreateNoticeRequestDto;
import team23.q_check.notice.dto.NoticeResponseDto;
import team23.q_check.notice.dto.UpdateNoticeRequestDto;

import java.util.List;

@Tag(name = "Notice", description = "클럽 공지사항 API")
@SecurityRequirement(name = "X-USER-ID")
@RestController
@RequestMapping("/api/clubs/{clubId}/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Operation(summary = "클럽 공지사항 목록 조회 (멤버 전용)")
    @GetMapping
    public ApiResponse<List<NoticeResponseDto>> getClubNotices(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long clubId
    ) {
        return ApiResponse.ok(noticeService.getClubNotices(currentUserId, clubId));
    }

    @Operation(summary = "클럽 공지사항 작성 (OWNER/ADMIN)")
    @PostMapping
    public ApiResponse<NoticeResponseDto> createNotice(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long clubId,
            @RequestBody CreateNoticeRequestDto request
    ) {
        return ApiResponse.ok(noticeService.createNotice(currentUserId, clubId, request));
    }

    @Operation(summary = "공지사항 수정 (OWNER/ADMIN)")
    @PutMapping("/{noticeId}")
    public ApiResponse<NoticeResponseDto> updateNotice(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long clubId,
            @PathVariable Long noticeId,
            @RequestBody UpdateNoticeRequestDto request
    ) {
        return ApiResponse.ok(noticeService.updateNotice(currentUserId, clubId, noticeId, request));
    }

    @Operation(summary = "공지사항 삭제 (OWNER/ADMIN)")
    @DeleteMapping("/{noticeId}")
    public ApiResponse<Void> deleteNotice(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long clubId,
            @PathVariable Long noticeId
    ) {
        noticeService.deleteNotice(currentUserId, clubId, noticeId);
        return ApiResponse.ok(null);
    }
}
