package team23.q_check.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team23.q_check.common.response.ApiResponse;
import team23.q_check.event.dto.EventResponseDto;
import team23.q_check.event.service.EventService;

@Tag(name = "Event", description = "Event API")
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @Operation(summary = "샘플 이벤트 조회")
    @GetMapping("/sample")
    public ApiResponse<EventResponseDto> getSampleEvent() {
        return ApiResponse.ok(eventService.getSampleEvent());
    }
}
