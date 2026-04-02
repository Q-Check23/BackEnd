package team23.q_check.event.dto;

import java.time.LocalDateTime;

public record CalendarEventItemDto(
        Long eventId,
        String eventTitle,
        Long clubId,
        String clubName,
        String location,
        LocalDateTime startTime
) {
}
