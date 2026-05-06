package team23.q_check.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "정산 항목 응답 DTO")
public record SettlementItemResponseDto(
        Long itemId,
        Long userId,
        String username,
        BigDecimal amount,
        String status,
        String updatedAt,
        String lastRemindedAt,
        long remindCount
) {
}
