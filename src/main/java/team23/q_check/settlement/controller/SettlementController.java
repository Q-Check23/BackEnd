package team23.q_check.settlement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team23.q_check.common.auth.CurrentUserId;
import team23.q_check.common.response.ApiResponse;
import team23.q_check.settlement.domain.service.SettlementService;
import team23.q_check.settlement.dto.CreateSettlementRequestDto;
import team23.q_check.settlement.dto.SettlementResponseDto;

@Tag(name = "Settlement", description = "정산 API")
@SecurityRequirement(name = "X-USER-ID")
@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @Operation(summary = "정산 생성 (club ADMIN 이상)",
            description = "그룹 단위로 사용자별 분담 금액을 입력합니다. " +
                    "예: 보증금 납부자 그룹 10000원, 미납자 그룹 20000원.")
    @PostMapping
    public ApiResponse<SettlementResponseDto> createSettlement(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @RequestBody CreateSettlementRequestDto request
    ) {
        return ApiResponse.ok(settlementService.createSettlement(currentUserId, request));
    }
}
