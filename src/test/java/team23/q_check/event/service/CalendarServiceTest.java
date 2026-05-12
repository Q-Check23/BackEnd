package team23.q_check.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import team23.q_check.club.domain.model.Club;
import team23.q_check.club.domain.model.ClubMember;
import team23.q_check.club.domain.model.ClubRole;
import team23.q_check.club.domain.repository.ClubMemberRepository;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.event.domain.model.Event;
import team23.q_check.event.domain.repository.EventRepository;
import team23.q_check.event.domain.repository.RegistrationRepository;
import team23.q_check.event.domain.service.CalendarService;
import team23.q_check.event.dto.CalendarClubGroupDto;
import team23.q_check.event.dto.CalendarEventItemDto;
import team23.q_check.identity.domain.model.User;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CalendarServiceTest {

    private ClubMemberRepository clubMemberRepository;
    private EventRepository eventRepository;
    private RegistrationRepository registrationRepository;
    private CalendarService calendarService;

    @BeforeEach
    void setUp() {
        clubMemberRepository = mock(ClubMemberRepository.class);
        eventRepository = mock(EventRepository.class);
        registrationRepository = mock(RegistrationRepository.class);
        calendarService = new CalendarService(clubMemberRepository, eventRepository, registrationRepository);
    }

    @Test
    void getMonthlyCalendar_invalidMonth_throwsBadRequest() {
        AppException tooLow = assertThrows(
                AppException.class,
                () -> calendarService.getMonthlyCalendar(1L, 2026, 0)
        );
        AppException tooHigh = assertThrows(
                AppException.class,
                () -> calendarService.getMonthlyCalendar(1L, 2026, 13)
        );
        assertEquals(ErrorCode.INVALID_REQUEST, tooLow.getErrorCode());
        assertEquals(ErrorCode.INVALID_REQUEST, tooHigh.getErrorCode());
    }

    @Test
    void getMonthlyCalendar_userWithoutClubs_returnsEmptyList() {
        when(clubMemberRepository.findAllByUser_Id(1L)).thenReturn(List.of());

        assertTrue(calendarService.getMonthlyCalendar(1L, 2026, 3).isEmpty());
    }

    @Test
    void getMonthlyCalendar_groupsEventsByClubAndSortsByStartTime() throws Exception {
        Club umc = newClub(10L, "UMC");
        Club gdsc = newClub(20L, "GDSC");
        User me = new User("dev-1", null, "kim", null);
        setId(me, 1L);
        when(clubMemberRepository.findAllByUser_Id(1L)).thenReturn(List.of(
                new ClubMember(umc, me, ClubRole.MEMBER),
                new ClubMember(gdsc, me, ClubRole.MEMBER)
        ));

        Event ot = newEvent(101L, umc, "OT", "2026-03-10T19:00:00");
        Event mt = newEvent(102L, umc, "MT", "2026-03-22T18:00:00");
        Event hack = newEvent(201L, gdsc, "Hackathon", "2026-03-15T09:00:00");
        when(eventRepository.findByClubIdsAndYearMonth(anyList(), eq(2026), eq(3)))
                .thenReturn(List.of(mt, ot, hack));

        List<CalendarClubGroupDto> result = calendarService.getMonthlyCalendar(1L, 2026, 3);

        assertEquals(2, result.size());
        // 멤버십 순서대로 그룹 (UMC, GDSC)
        CalendarClubGroupDto umcGroup = result.get(0);
        assertEquals("UMC", umcGroup.clubName());
        assertEquals(2, umcGroup.events().size());
        // 그룹 안에서는 startTime 오름차순
        assertEquals(101L, umcGroup.events().get(0).eventId());
        assertEquals(102L, umcGroup.events().get(1).eventId());

        CalendarClubGroupDto gdscGroup = result.get(1);
        assertEquals("GDSC", gdscGroup.clubName());
        assertEquals(1, gdscGroup.events().size());
    }

    @Test
    void getMonthlyCalendar_clubWithNoEvents_isOmittedFromResponse() throws Exception {
        Club umc = newClub(10L, "UMC");
        Club gdsc = newClub(20L, "GDSC");
        User me = new User("dev-1", null, "kim", null);
        setId(me, 1L);
        when(clubMemberRepository.findAllByUser_Id(1L)).thenReturn(List.of(
                new ClubMember(umc, me, ClubRole.MEMBER),
                new ClubMember(gdsc, me, ClubRole.MEMBER)
        ));
        Event onlyUmcEvent = newEvent(101L, umc, "OT", "2026-03-10T19:00:00");
        when(eventRepository.findByClubIdsAndYearMonth(anyList(), eq(2026), eq(3)))
                .thenReturn(List.of(onlyUmcEvent));

        List<CalendarClubGroupDto> result = calendarService.getMonthlyCalendar(1L, 2026, 3);

        assertEquals(1, result.size());
        assertEquals("UMC", result.get(0).clubName());
    }

    @Test
    void searchEvents_blankQuery_throwsBadRequest() {
        AppException exception = assertThrows(
                AppException.class,
                () -> calendarService.searchEvents(1L, " ")
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void searchEvents_noActiveRegistrations_returnsEmpty() {
        when(registrationRepository.findEventIdsByUserIdAndStatuses(eq(1L), anyList()))
                .thenReturn(List.of());

        assertTrue(calendarService.searchEvents(1L, "OT").isEmpty());
    }

    @Test
    void searchEvents_returnsRepositoryHitsAsDto() throws Exception {
        Club umc = newClub(10L, "UMC");
        Event ot = newEvent(101L, umc, "OT", "2026-03-10T19:00:00");
        when(registrationRepository.findEventIdsByUserIdAndStatuses(eq(1L), anyList()))
                .thenReturn(List.of(101L));
        when(eventRepository.searchInUserEvents(List.of(101L), "OT")).thenReturn(List.of(ot));

        List<CalendarEventItemDto> result = calendarService.searchEvents(1L, "  OT  ");

        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).eventId());
        assertEquals("OT", result.get(0).eventTitle());
    }

    @Test
    void filterEvents_allFiltersBlank_throwsBadRequest() {
        AppException exception = assertThrows(
                AppException.class,
                () -> calendarService.filterEvents(1L, null, null, null, "  ")
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void filterEvents_startAfterEnd_throwsBadRequest() {
        AppException exception = assertThrows(
                AppException.class,
                () -> calendarService.filterEvents(
                        1L,
                        LocalDateTime.parse("2026-03-20T00:00:00"),
                        LocalDateTime.parse("2026-03-10T00:00:00"),
                        null, null)
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void filterEvents_passesTrimmedNamesAndIds() throws Exception {
        Club umc = newClub(10L, "UMC");
        Event ot = newEvent(101L, umc, "OT", "2026-03-10T19:00:00");
        LocalDateTime start = LocalDateTime.parse("2026-03-01T00:00:00");
        LocalDateTime end = LocalDateTime.parse("2026-03-31T23:59:59");
        when(registrationRepository.findEventIdsByUserIdAndStatuses(eq(1L), anyList()))
                .thenReturn(List.of(101L));
        when(eventRepository.filterUserEvents(
                eq(List.of(101L)),
                eq(start),
                eq(end),
                eq("OT"),
                eq("UMC")
        )).thenReturn(List.of(ot));

        List<CalendarEventItemDto> result = calendarService.filterEvents(
                1L, start, end, "  OT  ", "  UMC  ");

        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).eventId());
    }

    private Club newClub(Long id, String name) throws Exception {
        Club club = new Club(name, "desc", "guild-" + id, null, "INV" + id);
        setId(club, id);
        return club;
    }

    private Event newEvent(Long id, Club club, String title, String startIso) throws Exception {
        Event event = new Event(club, title,
                LocalDateTime.parse(startIso),
                LocalDateTime.parse(startIso),
                null, true);
        setId(event, id);
        return event;
    }

    private void setId(Object target, Long id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
