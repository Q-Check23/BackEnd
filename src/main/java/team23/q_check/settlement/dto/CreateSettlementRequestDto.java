package team23.q_check.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "정산 생성 요청 DTO")
public record CreateSettlementRequestDto(
        @Schema(example = "1")
        Long eventId,
        @Schema(example = "OT 회식비")
        String title,
        @Schema(description = "총 결제 금액", example = "200000")
        BigDecimal totalAmount,
        @Schema(description = "영수증 이미지 URL (선택)", example = "https://cdn.example.com/r.jpg")
        String receiptImageUrl,
        @Schema(description = "분배 그룹 목록 — 보증금 낸 사람과 안 낸 사람을 별도 그룹으로 묶을 수 있음")
        List<SettlementGroupRequestDto> groups
) {
}
