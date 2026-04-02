package team23.q_check.event.dto;

import java.util.List;

public record CalendarClubGroupDto(
        Long clubId,
        String clubName,
        List<CalendarEventItemDto> events
) {
}
