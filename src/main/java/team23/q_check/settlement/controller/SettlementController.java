package team23.q_check.settlement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team23.q_check.common.auth.CurrentUserId;
import team23.q_check.common.response.ApiResponse;
import team23.q_check.settlement.domain.service.SettlementService;
import team23.q_check.settlement.dto.CreateSettlementRequestDto;
import team23.q_check.settlement.dto.SettlementItemResponseDto;
import team23.q_check.settlement.dto.SettlementResponseDto;
import team23.q_check.settlement.dto.SettlementSummaryDto;

import java.util.List;

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

    @Operation(summary = "행사별 정산 목록 조회 (club ADMIN 이상)")
    @GetMapping
    public ApiResponse<List<SettlementSummaryDto>> getEventSettlements(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @RequestParam Long eventId
    ) {
        return ApiResponse.ok(settlementService.getEventSettlements(currentUserId, eventId));
    }

    @Operation(summary = "정산 상세 조회 (club ADMIN 이상)")
    @GetMapping("/{settlementId}")
    public ApiResponse<SettlementResponseDto> getSettlement(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long settlementId
    ) {
        return ApiResponse.ok(settlementService.getSettlement(currentUserId, settlementId));
    }

    @Operation(summary = "본인 항목 송금 신고",
            description = "본인의 정산 항목 상태를 UNPAID에서 PENDING으로 변경합니다.")
    @PatchMapping("/items/{itemId}/mark-paid")
    public ApiResponse<SettlementItemResponseDto> markAsPending(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long itemId
    ) {
        return ApiResponse.ok(settlementService.markAsPending(currentUserId, itemId));
    }

    @Operation(summary = "송금 확인 처리 (club ADMIN 이상)",
            description = "정산 항목 상태를 PENDING에서 COMPLETED로 확정합니다.")
    @PatchMapping("/items/{itemId}/confirm")
    public ApiResponse<SettlementItemResponseDto> confirmAsCompleted(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long itemId
    ) {
        return ApiResponse.ok(settlementService.confirmAsCompleted(currentUserId, itemId));
    }
}
