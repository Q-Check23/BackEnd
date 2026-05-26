package team23.q_check.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import team23.q_check.club.domain.model.Club;
import team23.q_check.club.domain.model.ClubMember;
import team23.q_check.club.domain.model.ClubRole;
import team23.q_check.club.domain.repository.ClubRepository;
import team23.q_check.club.domain.service.ClubAuthorizationService;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.event.domain.service.EventService;
import team23.q_check.identity.domain.model.User;
import team23.q_check.event.domain.model.Event;
import team23.q_check.event.domain.model.form.FormField;
import team23.q_check.event.dto.CreateEventRequestDto;
import team23.q_check.event.dto.FormFieldRequestDto;
import team23.q_check.event.dto.UpdateEventRequestDto;
import team23.q_check.event.domain.repository.AttendanceLogRepository;
import team23.q_check.event.domain.repository.EventPhotoRepository;
import team23.q_check.event.domain.repository.EventRepository;
import team23.q_check.event.domain.repository.FormAnswerRepository;
import team23.q_check.event.domain.repository.FormFieldRepository;
import team23.q_check.event.domain.repository.RegistrationRepository;
import team23.q_check.integrations.domain.service.DiscordBotService;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventServiceTest {

    private EventRepository eventRepository;
    private FormFieldRepository formFieldRepository;
    private RegistrationRepository registrationRepository;
    private FormAnswerRepository formAnswerRepository;
    private AttendanceLogRepository attendanceLogRepository;
    private EventPhotoRepository eventPhotoRepository;
    private ClubRepository clubRepository;
    private ClubAuthorizationService clubAuthorizationService;
    private DiscordBotService discordBotService;
    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        formFieldRepository = mock(FormFieldRepository.class);
        registrationRepository = mock(RegistrationRepository.class);
        formAnswerRepository = mock(FormAnswerRepository.class);
        attendanceLogRepository = mock(AttendanceLogRepository.class);
        eventPhotoRepository = mock(EventPhotoRepository.class);
        clubRepository = mock(ClubRepository.class);
        clubAuthorizationService = mock(ClubAuthorizationService.class);
        discordBotService = mock(DiscordBotService.class);
        eventService = new EventService(
                eventRepository,
                formFieldRepository,
                registrationRepository,
                formAnswerRepository,
                attendanceLogRepository,
                eventPhotoRepository,
                clubRepository,
                clubAuthorizationService,
                discordBotService,
                new ObjectMapper()
        );
    }

    @Test
    void createEvent_withSelectField_savesAndReturnsDetail() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null, "INV12345");
        setId(club, 1L);
        User admin = new User("dev-1", null, "admin", null);
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
                        null,
                        "2026-03-10T19:00:00",
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(new FormFieldRequestDto("SELECT", "식사 메뉴", true, List.of("한식", "양식")))
                )
        );

        assertEquals(100L, result.eventId());
        assertEquals(1, result.formFields().size());
        assertEquals("SELECT", result.formFields().get(0).type());
    }

    @Test
    void createEvent_selectWithoutOptions_throwsInvalidRequest() {
        Club club = new Club("UMC", "desc", "guild-1", null, "INV12345");
        User admin = new User("dev-1", null, "admin", null);
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
                                null,
                                "2026-03-10T19:00:00",
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of(new FormFieldRequestDto("SELECT", "식사 메뉴", true, List.of()))
                        )
                )
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void createEvent_propagatesAllFieldsToEntity() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null, "INV12345");
        setId(club, 1L);
        User admin = new User("dev-1", null, "admin", null);
        ClubMember adminMembership = new ClubMember(club, admin, ClubRole.ADMIN);
        when(clubRepository.existsById(1L)).thenReturn(true);
        when(clubAuthorizationService.requireAdminOrOwner(1L, 1L)).thenReturn(adminMembership);

        Event saved = new Event(club, "OT",
                LocalDateTime.parse("2026-03-10T19:00:00"),
                LocalDateTime.parse("2026-03-10T22:00:00"),
                null, true);
        setId(saved, 100L);
        when(eventRepository.save(any(Event.class))).thenReturn(saved);
        when(formFieldRepository.findAllByEvent_IdOrderBySortOrderAsc(100L)).thenReturn(List.of());

        eventService.createEvent(
                1L,
                new CreateEventRequestDto(
                        1L,
                        "OT",
                        "신입 환영회",
                        "2026-03-10T19:00:00",
                        "2026-03-10T22:00:00",
                        "강남",
                        true,
                        "1234567890",
                        new BigDecimal("10000"),
                        List.of()
                )
        );

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        Event captured = captor.getValue();
        assertEquals("OT", captured.getTitle());
        assertEquals("신입 환영회", captured.getDescription());
        assertEquals(LocalDateTime.parse("2026-03-10T19:00:00"), captured.getStartTime());
        assertEquals(LocalDateTime.parse("2026-03-10T22:00:00"), captured.getEndTime());
        assertEquals("강남", captured.getLocation());
        assertEquals("1234567890", captured.getDiscordChannelId());
        assertEquals(new BigDecimal("10000"), captured.getRegisterFee());
    }

    @Test
    void createEvent_endTimeBeforeStart_throwsInvalidRequest() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null, "INV12345");
        setId(club, 1L);
        ClubMember adminMembership = new ClubMember(club, new User("dev-1", null, "admin", null), ClubRole.ADMIN);
        when(clubRepository.existsById(1L)).thenReturn(true);
        when(clubAuthorizationService.requireAdminOrOwner(1L, 1L)).thenReturn(adminMembership);

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.createEvent(1L, new CreateEventRequestDto(
                        1L, "OT", null,
                        "2026-03-10T19:00:00", "2026-03-10T18:00:00",
                        null, null, null, null, List.of()
                ))
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void createEvent_negativeRegisterFee_throwsInvalidRequest() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null, "INV12345");
        setId(club, 1L);
        ClubMember adminMembership = new ClubMember(club, new User("dev-1", null, "admin", null), ClubRole.ADMIN);
        when(clubRepository.existsById(1L)).thenReturn(true);
        when(clubAuthorizationService.requireAdminOrOwner(1L, 1L)).thenReturn(adminMembership);

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.createEvent(1L, new CreateEventRequestDto(
                        1L, "OT", null, "2026-03-10T19:00:00", null, null, null, null,
                        new BigDecimal("-1"), List.of()
                ))
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void updateEvent_withEndTimeOnly_doesNotOverwriteStartTime() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null, "INV12345");
        setId(club, 1L);
        Event event = new Event(club, "OT",
                LocalDateTime.parse("2026-03-10T19:00:00"),
                LocalDateTime.parse("2026-03-10T20:00:00"),
                "강남", true);
        setId(event, 100L);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(formFieldRepository.findAllByEvent_IdOrderBySortOrderAsc(100L)).thenReturn(List.of());

        eventService.updateEvent(1L, 100L, new UpdateEventRequestDto(
                null, null, null, "2026-03-10T22:00:00", null, null, null, null
        ));

        assertEquals(LocalDateTime.parse("2026-03-10T19:00:00"), event.getStartTime());
        assertEquals(LocalDateTime.parse("2026-03-10T22:00:00"), event.getEndTime());
    }

    @Test
    void updateEvent_endTimeBeforeExistingStart_throwsInvalidRequest() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null, "INV12345");
        setId(club, 1L);
        Event event = new Event(club, "OT",
                LocalDateTime.parse("2026-03-10T19:00:00"),
                LocalDateTime.parse("2026-03-10T20:00:00"),
                null, true);
        setId(event, 100L);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.updateEvent(1L, 100L, new UpdateEventRequestDto(
                        null, null, null, "2026-03-10T18:00:00", null, null, null, null
                ))
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void deleteEvent_whenActiveRegistrationExists_throwsConflict() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null, "INV12345");
        setId(club, 1L);
        Event event = new Event(club, "OT",
                LocalDateTime.parse("2026-03-10T19:00:00"),
                LocalDateTime.parse("2026-03-10T20:00:00"),
                null, true);
        setId(event, 100L);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByEvent_IdAndStatusIn(
                org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.anyList()
        )).thenReturn(true);

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.deleteEvent(1L, 100L)
        );
        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    @Test
    void deleteEvent_cascadesChildrenAndDeletesEvent() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null, "INV12345");
        setId(club, 1L);
        Event event = new Event(club, "OT",
                LocalDateTime.parse("2026-03-10T19:00:00"),
                LocalDateTime.parse("2026-03-10T20:00:00"),
                null, true);
        setId(event, 100L);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByEvent_IdAndStatusIn(
                org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.anyList()
        )).thenReturn(false);

        eventService.deleteEvent(1L, 100L);

        verify(attendanceLogRepository).deleteAllByEvent_Id(100L);
        verify(formAnswerRepository).deleteAllByRegistration_Event_Id(100L);
        verify(registrationRepository).deleteAllByEvent_Id(100L);
        verify(formFieldRepository).deleteAllByEvent_Id(100L);
        verify(eventPhotoRepository).deleteAllByEvent_Id(100L);
        verify(eventRepository).delete(event);
    }

    @Test
    void event_defaultConstructor_initializesRegisterFeeToZero() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null, "INV12345");
        Event event = new Event(club, "OT",
                LocalDateTime.parse("2026-03-10T19:00:00"),
                LocalDateTime.parse("2026-03-10T20:00:00"),
                null, true);
        assertEquals(BigDecimal.ZERO, event.getRegisterFee());
        assertNull(event.getDescription());
        assertNull(event.getDiscordChannelId());
    }

    @Test
    void updateEvent_whenEventNotFound_throwsNotFound() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.updateEvent(1L, 999L,
                        new UpdateEventRequestDto(null, null, null, null, "강남", null, null, false))
        );
        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    private void setId(Object target, Long id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
