package team23.q_check.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import team23.q_check.club.domain.model.Club;
import team23.q_check.club.domain.model.ClubMember;
import team23.q_check.club.domain.model.ClubRole;
import team23.q_check.club.service.ClubAuthorizationService;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.event.domain.model.Event;
import team23.q_check.event.domain.model.Registration;
import team23.q_check.event.domain.model.form.FieldType;
import team23.q_check.event.domain.model.form.FormAnswer;
import team23.q_check.event.domain.model.form.FormField;
import team23.q_check.event.dto.CreateRegistrationRequestDto;
import team23.q_check.event.dto.RegistrationAnswerRequestDto;
import team23.q_check.event.repository.EventRepository;
import team23.q_check.event.repository.FormAnswerRepository;
import team23.q_check.event.repository.FormFieldRepository;
import team23.q_check.event.repository.RegistrationRepository;
import team23.q_check.identity.domain.model.User;
import team23.q_check.identity.repository.UserRepository;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegistrationServiceTest {

    private EventRepository eventRepository;
    private RegistrationRepository registrationRepository;
    private FormFieldRepository formFieldRepository;
    private FormAnswerRepository formAnswerRepository;
    private UserRepository userRepository;
    private ClubAuthorizationService clubAuthorizationService;
    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        registrationRepository = mock(RegistrationRepository.class);
        formFieldRepository = mock(FormFieldRepository.class);
        formAnswerRepository = mock(FormAnswerRepository.class);
        userRepository = mock(UserRepository.class);
        clubAuthorizationService = mock(ClubAuthorizationService.class);
        registrationService = new RegistrationService(
                eventRepository,
                registrationRepository,
                formFieldRepository,
                formAnswerRepository,
                userRepository,
                clubAuthorizationService,
                new ObjectMapper()
        );
    }

    @Test
    void createRegistration_whenEventInactive_throwsConflict() {
        Event event = new Event(new Club("UMC", "desc", "guild-1", null), "OT",
                LocalDateTime.parse("2026-03-10T19:00:00"),
                LocalDateTime.parse("2026-03-10T19:00:00"),
                null,
                false);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));

        AppException exception = assertThrows(
                AppException.class,
                () -> registrationService.createRegistration(1L, 100L, new CreateRegistrationRequestDto(List.of()))
        );
        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    @Test
    void createRegistration_whenRequiredFieldMissing_throwsBadRequest() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null);
        Event event = new Event(club, "OT",
                LocalDateTime.parse("2026-03-10T19:00:00"),
                LocalDateTime.parse("2026-03-10T19:00:00"),
                null,
                true);
        setId(event, 100L);

        FormField requiredField = new FormField(event, 0L, "학번", FieldType.TEXT, null, true);
        setId(requiredField, 11L);

        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByEvent_IdAndUser_Id(100L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User("dev-1", "kim")));
        when(formFieldRepository.findAllByEvent_IdOrderBySortOrderAsc(100L)).thenReturn(List.of(requiredField));

        AppException exception = assertThrows(
                AppException.class,
                () -> registrationService.createRegistration(
                        1L,
                        100L,
                        new CreateRegistrationRequestDto(List.of())
                )
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void createRegistration_whenSelectOutOfOptions_throwsBadRequest() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null);
        Event event = new Event(club, "OT",
                LocalDateTime.parse("2026-03-10T19:00:00"),
                LocalDateTime.parse("2026-03-10T19:00:00"),
                null,
                true);
        setId(event, 100L);

        FormField selectField = new FormField(event, 0L, "식사 메뉴", FieldType.SELECT, "[\"한식\",\"양식\"]", true);
        setId(selectField, 11L);

        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByEvent_IdAndUser_Id(100L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User("dev-1", "kim")));
        when(formFieldRepository.findAllByEvent_IdOrderBySortOrderAsc(100L)).thenReturn(List.of(selectField));

        AppException exception = assertThrows(
                AppException.class,
                () -> registrationService.createRegistration(
                        1L,
                        100L,
                        new CreateRegistrationRequestDto(List.of(
                                new RegistrationAnswerRequestDto(11L, "중식")
                        ))
                )
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void createRegistration_whenDuplicate_throwsConflict() {
        Event event = new Event(new Club("UMC", "desc", "guild-1", null), "OT",
                LocalDateTime.parse("2026-03-10T19:00:00"),
                LocalDateTime.parse("2026-03-10T19:00:00"),
                null,
                true);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByEvent_IdAndUser_Id(100L, 1L)).thenReturn(true);

        AppException exception = assertThrows(
                AppException.class,
                () -> registrationService.createRegistration(1L, 100L, new CreateRegistrationRequestDto(List.of()))
        );
        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    @Test
    void getEventRegistrations_requiresAdminRole() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null);
        setId(club, 1L);
        Event event = new Event(club, "OT",
                LocalDateTime.parse("2026-03-10T19:00:00"),
                LocalDateTime.parse("2026-03-10T19:00:00"),
                null,
                true);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(clubAuthorizationService.requireAdminOrOwner(1L, 1L))
                .thenThrow(new AppException(ErrorCode.FORBIDDEN, "forbidden"));

        AppException exception = assertThrows(
                AppException.class,
                () -> registrationService.getEventRegistrations(1L, 100L)
        );
        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void getMyRegistration_returnsAnswers() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null);
        Event event = new Event(club, "OT",
                LocalDateTime.parse("2026-03-10T19:00:00"),
                LocalDateTime.parse("2026-03-10T19:00:00"),
                null,
                true);
        User user = new User("dev-1", "kim");
        setId(user, 1L);

        Registration registration = new Registration(event, user, "token-1", team23.q_check.event.domain.model.RegistrationStatus.REGISTERED);
        setId(registration, 200L);

        FormField field = new FormField(event, 0L, "식사 메뉴", FieldType.SELECT, "[\"한식\",\"양식\"]", true);
        setId(field, 11L);
        FormAnswer answer = new FormAnswer(registration, field, "한식");

        when(registrationRepository.findByEvent_IdAndUser_Id(100L, 1L)).thenReturn(Optional.of(registration));
        when(formAnswerRepository.findAllByRegistration_Id(200L)).thenReturn(List.of(answer));

        var result = registrationService.getMyRegistration(1L, 100L);
        assertEquals(200L, result.registrationId());
        assertEquals("REGISTERED", result.status());
        assertEquals("한식", result.answers().get(0).value());
    }

    private void setId(Object target, Long id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
