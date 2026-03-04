package team23.q_check.event.service;

import org.springframework.stereotype.Service;
import team23.q_check.event.dto.EventResponseDto;

@Service
public class EventService {

    public EventResponseDto getSampleEvent() {
        return new EventResponseDto(10L, "Spring Boot Study", "2026-03-05T19:00:00");
    }
}
