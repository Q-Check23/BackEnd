package team23.q_check.event.controller;

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
import team23.q_check.event.dto.CheckInRequestDto;
import team23.q_check.event.dto.CheckInResponseDto;
import team23.q_check.event.dto.ManualCheckInRequestDto;
import team23.q_check.event.domain.service.AttendanceService;

@Tag(name = "Attendance", description = "Attendance API")
@SecurityRequirement(name = "X-USER-ID")
@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @Operation(summary = "QR 체크인")
    @PostMapping("/check-in")
    public ApiResponse<CheckInResponseDto> checkIn(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @RequestBody CheckInRequestDto request
    ) {
        return ApiResponse.ok(attendanceService.checkIn(currentUserId, request.qrToken()));
    }

    @Operation(summary = "수동 체크인 (club ADMIN 이상)",
            description = "QR 인식 실패·지각 등으로 관리자가 직접 출석 처리할 때 사용합니다. " +
                    "참가 목록(GET /api/events/{eventId}/registrations)에서 얻은 " +
                    "registrationId를 그대로 사용하면 됩니다. " +
                    "이미 CHECKED_IN 이면 409.")
    @PostMapping("/manual")
    public ApiResponse<CheckInResponseDto> manualCheckIn(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @RequestBody ManualCheckInRequestDto request
    ) {
        return ApiResponse.ok(attendanceService.manualCheckIn(currentUserId, request.registrationId()));
    }
}
