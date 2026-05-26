package team23.q_check.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "아이디 중복 확인 응답 DTO")
public record UsernameCheckResponseDto(
        @Schema(description = "사용 가능 여부. true이면 사용 가능")
        boolean available
) {}
