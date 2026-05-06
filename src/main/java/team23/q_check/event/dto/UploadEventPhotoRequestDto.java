package team23.q_check.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "행사 사진 업로드 요청 DTO (URL 전용)")
public record UploadEventPhotoRequestDto(
        @Schema(description = "외부 스토리지에 이미 업로드된 이미지의 URL",
                example = "https://cdn.example.com/photos/abc.jpg")
        String photoUrl
) {
}
