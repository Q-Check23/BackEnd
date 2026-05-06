package team23.q_check.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team23.q_check.common.auth.CurrentUserId;
import team23.q_check.common.response.ApiResponse;
import team23.q_check.event.domain.service.EventPhotoService;
import team23.q_check.event.dto.EventPhotoResponseDto;
import team23.q_check.event.dto.UploadEventPhotoRequestDto;

import java.util.List;

@Tag(name = "Event Photo", description = "행사 사진 API")
@SecurityRequirement(name = "X-USER-ID")
@RestController
@RequestMapping("/api/events/{eventId}/photos")
public class EventPhotoController {

    private final EventPhotoService eventPhotoService;

    public EventPhotoController(EventPhotoService eventPhotoService) {
        this.eventPhotoService = eventPhotoService;
    }

    @Operation(summary = "행사 사진 업로드 (URL 등록)",
            description = "외부 스토리지에 이미 업로드된 이미지의 URL을 행사에 첨부합니다. " +
                    "클럽 멤버 누구나 가능합니다.")
    @PostMapping
    public ApiResponse<EventPhotoResponseDto> uploadPhoto(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long eventId,
            @RequestBody UploadEventPhotoRequestDto request
    ) {
        return ApiResponse.ok(eventPhotoService.uploadPhoto(currentUserId, eventId, request));
    }

    @Operation(summary = "행사 사진 목록 조회",
            description = "최신순으로 정렬해서 반환합니다. 클럽 멤버 누구나 조회 가능.")
    @GetMapping
    public ApiResponse<List<EventPhotoResponseDto>> getEventPhotos(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long eventId
    ) {
        return ApiResponse.ok(eventPhotoService.getEventPhotos(currentUserId, eventId));
    }

    @Operation(summary = "행사 사진 삭제",
            description = "본인이 업로드한 사진이거나 클럽 ADMIN/OWNER일 때만 삭제할 수 있습니다.")
    @DeleteMapping("/{photoId}")
    public ApiResponse<Void> deletePhoto(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long eventId,
            @PathVariable Long photoId
    ) {
        eventPhotoService.deletePhoto(currentUserId, eventId, photoId);
        return ApiResponse.ok(null);
    }
}
