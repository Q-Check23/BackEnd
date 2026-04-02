package team23.q_check.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 토큰 응답 DTO (refresh token은 쿠키에 포함)")
public record AuthTokenResponseDto(
        @Schema(example = "1")
        Long userId,
        @Schema(description = "JWT access token")
        String accessToken
) {}
