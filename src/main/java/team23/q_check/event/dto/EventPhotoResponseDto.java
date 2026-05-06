package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "행사 사진 응답 DTO")
public record EventPhotoResponseDto(
        @Schema(example = "501")
        Long photoId,
        @Schema(example = "https://cdn.example.com/photos/abc.jpg")
        String photoUrl,
        @Schema(example = "7")
        Long uploaderUserId,
        @Schema(example = "kimjyun")
        String uploaderUsername,
        @Schema(example = "2026-05-07T19:00:00")
        String createdAt
) {
}
