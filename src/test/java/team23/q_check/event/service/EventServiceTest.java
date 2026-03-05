package team23.q_check.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import team23.q_check.club.domain.model.Club;
import team23.q_check.club.domain.model.ClubMember;
import team23.q_check.club.domain.model.ClubRole;
import team23.q_check.club.repository.ClubRepository;
import team23.q_check.club.service.ClubAuthorizationService;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.identity.domain.model.User;
import team23.q_check.event.domain.model.Event;
import team23.q_check.event.domain.model.form.FormField;
import team23.q_check.event.dto.CreateEventRequestDto;
import team23.q_check.event.dto.FormFieldRequestDto;
import team23.q_check.event.dto.UpdateEventRequestDto;
import team23.q_check.event.repository.EventRepository;
import team23.q_check.event.repository.FormFieldRepository;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventServiceTest {

    private EventRepository eventRepository;
    private FormFieldRepository formFieldRepository;
    private ClubRepository clubRepository;
    private ClubAuthorizationService clubAuthorizationService;
    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        formFieldRepository = mock(FormFieldRepository.class);
        clubRepository = mock(ClubRepository.class);
        clubAuthorizationService = mock(ClubAuthorizationService.class);
        eventService = new EventService(
                eventRepository,
                formFieldRepository,
                clubRepository,
                clubAuthorizationService,
                new ObjectMapper()
        );
    }

    @Test
    void createEvent_withSelectField_savesAndReturnsDetail() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null);
        setId(club, 1L);
        User admin = new User("dev-1", "admin");
        setId(admin, 1L);
        ClubMember adminMembership = new ClubMember(club, admin, ClubRole.ADMIN);

        Event savedEvent = new Event(club, "OT", java.time.LocalDateTime.parse("2026-03-10T19:00:00"),
                java.time.LocalDateTime.parse("2026-03-10T19:00:00"), null, true);
        setId(savedEvent, 100L);

        FormField savedField = new FormField(savedEvent, 0L, "식사 메뉴",
                team23.q_check.event.domain.model.form.FieldType.SELECT,
                "[\"한식\",\"양식\"]", true);
        setId(savedField, 10L);

        when(clubRepository.existsById(1L)).thenReturn(true);
        when(clubAuthorizationService.requireAdminOrOwner(1L, 1L)).thenReturn(adminMembership);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
        when(formFieldRepository.findAllByEvent_IdOrderBySortOrderAsc(100L)).thenReturn(List.of(savedField));

        var result = eventService.createEvent(
                1L,
                new CreateEventRequestDto(
                        1L,
                        "OT",
                        "2026-03-10T19:00:00",
                        List.of(new FormFieldRequestDto("SELECT", "식사 메뉴", true, List.of("한식", "양식")))
                )
        );

        assertEquals(100L, result.eventId());
        assertEquals(1, result.formFields().size());
        assertEquals("SELECT", result.formFields().get(0).type());
    }

    @Test
    void createEvent_selectWithoutOptions_throwsInvalidRequest() {
        Club club = new Club("UMC", "desc", "guild-1", null);
        User admin = new User("dev-1", "admin");
        ClubMember adminMembership = new ClubMember(club, admin, ClubRole.ADMIN);
        when(clubRepository.existsById(1L)).thenReturn(true);
        when(clubAuthorizationService.requireAdminOrOwner(1L, 1L)).thenReturn(adminMembership);

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.createEvent(
                        1L,
                        new CreateEventRequestDto(
                                1L,
                                "OT",
                                "2026-03-10T19:00:00",
                                List.of(new FormFieldRequestDto("SELECT", "식사 메뉴", true, List.of()))
                        )
                )
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void updateEvent_whenEventNotFound_throwsNotFound() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.updateEvent(1L, 999L, new UpdateEventRequestDto(null, null, "강남", false))
        );
        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    private void setId(Object target, Long id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
