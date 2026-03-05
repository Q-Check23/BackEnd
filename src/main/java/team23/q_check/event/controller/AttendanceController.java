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
import team23.q_check.event.service.AttendanceService;

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
}
