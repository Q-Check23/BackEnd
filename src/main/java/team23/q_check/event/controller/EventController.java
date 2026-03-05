package team23.q_check.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team23.q_check.common.auth.CurrentUserId;
import team23.q_check.common.response.ApiResponse;
import team23.q_check.event.dto.CreateEventRequestDto;
import team23.q_check.event.dto.CreateRegistrationRequestDto;
import team23.q_check.event.dto.CreateRegistrationResponseDto;
import team23.q_check.event.dto.AdminRegistrationItemDto;
import team23.q_check.event.dto.EventDetailResponseDto;
import team23.q_check.event.dto.EventPageResponseDto;
import team23.q_check.event.dto.MyRegistrationResponseDto;
import team23.q_check.event.dto.UpdateEventRequestDto;
import team23.q_check.event.service.EventService;
import team23.q_check.event.service.RegistrationService;

import java.util.List;

@Tag(name = "Event", description = "Event API")
@SecurityRequirement(name = "X-USER-ID")
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final RegistrationService registrationService;

    public EventController(EventService eventService, RegistrationService registrationService) {
        this.eventService = eventService;
        this.registrationService = registrationService;
    }

    @Operation(summary = "행사 생성 (club ADMIN 이상)")
    @PostMapping
    public ApiResponse<EventDetailResponseDto> createEvent(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @RequestBody CreateEventRequestDto request
    ) {
        return ApiResponse.ok(eventService.createEvent(currentUserId, request));
    }

    @Operation(summary = "행사 목록 조회 (페이징)")
    @GetMapping
    public ApiResponse<EventPageResponseDto> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(eventService.getEvents(page, size));
    }

    @Operation(summary = "행사 상세 조회 (formFields 포함)")
    @GetMapping("/{eventId}")
    public ApiResponse<EventDetailResponseDto> getEvent(@PathVariable Long eventId) {
        return ApiResponse.ok(eventService.getEvent(eventId));
    }

    @Operation(summary = "행사 수정 (club ADMIN 이상)")
    @PutMapping("/{eventId}")
    public ApiResponse<EventDetailResponseDto> updateEvent(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long eventId,
            @RequestBody UpdateEventRequestDto request
    ) {
        return ApiResponse.ok(eventService.updateEvent(currentUserId, eventId, request));
    }

    @Operation(summary = "행사 참가 신청")
    @PostMapping("/{eventId}/registrations")
    public ApiResponse<CreateRegistrationResponseDto> createRegistration(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long eventId,
            @RequestBody CreateRegistrationRequestDto request
    ) {
        return ApiResponse.ok(registrationService.createRegistration(currentUserId, eventId, request));
    }

    @Operation(summary = "내 참가 신청 조회")
    @GetMapping("/{eventId}/registrations/me")
    public ApiResponse<MyRegistrationResponseDto> getMyRegistration(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long eventId
    ) {
        return ApiResponse.ok(registrationService.getMyRegistration(currentUserId, eventId));
    }

    @Operation(summary = "행사 참가자 목록 조회 (club ADMIN 이상)")
    @GetMapping("/{eventId}/registrations")
    public ApiResponse<List<AdminRegistrationItemDto>> getEventRegistrations(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long eventId
    ) {
        return ApiResponse.ok(registrationService.getEventRegistrations(currentUserId, eventId));
    }
}
