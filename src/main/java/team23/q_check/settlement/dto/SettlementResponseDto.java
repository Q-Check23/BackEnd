package team23.q_check.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "정산 상세 응답 DTO")
public record SettlementResponseDto(
        Long settlementId,
        Long eventId,
        Long createdByUserId,
        String title,
        BigDecimal totalAmount,
        String receiptImageUrl,
        String createdAt,
        BigDecimal allocatedAmount,
        BigDecimal unallocatedAmount,
        List<SettlementItemResponseDto> items
) {
}
