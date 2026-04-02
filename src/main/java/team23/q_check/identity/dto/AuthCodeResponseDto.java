package team23.q_check.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Discord 인가코드 처리 응답 DTO")
public record AuthCodeResponseDto(
        @Schema(description = "신규 회원 여부. true이면 token은 회원가입용 JWT, false이면 access token")
        boolean isNewUser,
        @Schema(description = "신규 회원: signup JWT / 기존 회원: access JWT")
        String token
) {}
