package team23.q_check.event.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team23.q_check.club.domain.model.Club;
import team23.q_check.club.domain.model.ClubMember;
import team23.q_check.club.domain.repository.ClubMemberRepository;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.event.domain.model.Event;
import team23.q_check.event.domain.model.RegistrationStatus;
import team23.q_check.event.domain.repository.EventRepository;
import team23.q_check.event.domain.repository.RegistrationRepository;
import team23.q_check.event.dto.CalendarClubGroupDto;
import team23.q_check.event.dto.CalendarEventItemDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CalendarService {

    private static final List<RegistrationStatus> ACTIVE_STATUSES =
            List.of(RegistrationStatus.REGISTERED, RegistrationStatus.CHECKED_IN);

    private final ClubMemberRepository clubMemberRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    public CalendarService(
            ClubMemberRepository clubMemberRepository,
            EventRepository eventRepository,
            RegistrationRepository registrationRepository
    ) {
        this.clubMemberRepository = clubMemberRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }

    @Transactional(readOnly = true)
    public List<CalendarClubGroupDto> getMonthlyCalendar(Long userId, int year, int month) {
        if (month < 1 || month > 12) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "month must be between 1 and 12");
        }

        List<ClubMember> memberships = clubMemberRepository.findAllByUser_Id(userId);
        if (memberships.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> clubIds = memberships.stream()
                .map(m -> m.getClub().getId())
                .toList();

        List<Event> events = eventRepository.findByClubIdsAndYearMonth(clubIds, year, month);
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<Event>> byClub = events.stream()
                .collect(Collectors.groupingBy(e -> e.getClub().getId()));

        return memberships.stream()
                .filter(m -> byClub.containsKey(m.getClub().getId()))
                .map(m -> {
                    Club club = m.getClub();
                    List<CalendarEventItemDto> eventDtos = byClub.get(club.getId()).stream()
                            .sorted(Comparator.comparing(Event::getStartTime))
                            .map(this::toEventItemDto)
                            .toList();
                    return new CalendarClubGroupDto(club.getId(), club.getName(), eventDtos);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CalendarEventItemDto> searchEvents(Long userId, String query) {
        if (query == null || query.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "query is required");
        }

        List<Long> eventIds = registrationRepository.findEventIdsByUserIdAndStatuses(userId, ACTIVE_STATUSES);
        if (eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        return eventRepository.searchInUserEvents(eventIds, query.trim()).stream()
                .map(this::toEventItemDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CalendarEventItemDto> filterEvents(
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String eventName,
            String clubName
    ) {
        if (startDate == null && endDate == null && isBlank(eventName) && isBlank(clubName)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "At least one filter parameter is required");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "startDate must not be after endDate");
        }

        List<Long> eventIds = registrationRepository.findEventIdsByUserIdAndStatuses(userId, ACTIVE_STATUSES);
        if (eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        return eventRepository.filterUserEvents(
                eventIds,
                startDate,
                endDate,
                isBlank(eventName) ? null : eventName.trim(),
                isBlank(clubName) ? null : clubName.trim()
        ).stream().map(this::toEventItemDto).toList();
    }

    private CalendarEventItemDto toEventItemDto(Event event) {
        return new CalendarEventItemDto(
                event.getId(),
                event.getTitle(),
                event.getClub().getId(),
                event.getClub().getName(),
                event.getLocation(),
                event.getStartTime()
        );
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
