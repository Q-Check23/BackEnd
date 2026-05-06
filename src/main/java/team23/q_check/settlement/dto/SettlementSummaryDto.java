package team23.q_check.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "정산 목록 요약 DTO")
public record SettlementSummaryDto(
        Long settlementId,
        Long eventId,
        String title,
        BigDecimal totalAmount,
        String createdAt,
        long itemCount,
        long completedCount
) {
}
