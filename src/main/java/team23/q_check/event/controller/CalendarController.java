package team23.q_check.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team23.q_check.common.auth.CurrentUserId;
import team23.q_check.common.response.ApiResponse;
import team23.q_check.event.domain.service.CalendarService;
import team23.q_check.event.dto.CalendarClubGroupDto;
import team23.q_check.event.dto.CalendarEventItemDto;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Calendar", description = "일정 관리 API")
@SecurityRequirement(name = "X-USER-ID")
@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @Operation(summary = "월별 일정 조회 - 내가 속한 동아리의 행사를 동아리별로 그룹화하여 반환")
    @GetMapping
    public ApiResponse<List<CalendarClubGroupDto>> getMonthlyCalendar(
            @CurrentUserId Long currentUserId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ApiResponse.ok(calendarService.getMonthlyCalendar(currentUserId, year, month));
    }

    @Operation(summary = "행사 검색 - 행사명·동아리명·장소명으로 검색 (내가 참가한/예정인 행사 대상)")
    @GetMapping("/search")
    public ApiResponse<List<CalendarEventItemDto>> searchEvents(
            @CurrentUserId Long currentUserId,
            @RequestParam String query
    ) {
        return ApiResponse.ok(calendarService.searchEvents(currentUserId, query));
    }

    @Operation(summary = "행사 필터링 - 기간·행사명·동아리명으로 필터링 (내가 참가한/예정인 행사 대상)")
    @GetMapping("/filter")
    public ApiResponse<List<CalendarEventItemDto>> filterEvents(
            @CurrentUserId Long currentUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String eventName,
            @RequestParam(required = false) String clubName
    ) {
        return ApiResponse.ok(calendarService.filterEvents(currentUserId, startDate, endDate, eventName, clubName));
    }
}
