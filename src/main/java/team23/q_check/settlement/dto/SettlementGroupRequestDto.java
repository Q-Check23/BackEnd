package team23.q_check.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "정산 그룹 — 같은 금액을 적용할 사용자 묶음")
public record SettlementGroupRequestDto(
        @Schema(description = "이 그룹에 포함될 사용자 ID", example = "[1, 2, 3]")
        List<Long> userIds,
        @Schema(description = "이 그룹의 1인당 금액", example = "10000")
        BigDecimal amount
) {
}
