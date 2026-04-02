package team23.q_check.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import team23.q_check.club.domain.model.Club;
import team23.q_check.club.domain.service.ClubAuthorizationService;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.event.domain.model.Event;
import team23.q_check.event.domain.model.Registration;
import team23.q_check.event.domain.model.RegistrationStatus;
import team23.q_check.event.domain.repository.AttendanceLogRepository;
import team23.q_check.event.domain.repository.RegistrationRepository;
import team23.q_check.event.domain.service.AttendanceService;
import team23.q_check.identity.domain.model.User;
import team23.q_check.identity.domain.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AttendanceServiceTest {

    private RegistrationRepository registrationRepository;
    private AttendanceLogRepository attendanceLogRepository;
    private UserRepository userRepository;
    private ClubAuthorizationService clubAuthorizationService;
    private AttendanceService attendanceService;

    @BeforeEach
    void setUp() {
        registrationRepository = mock(RegistrationRepository.class);
        attendanceLogRepository = mock(AttendanceLogRepository.class);
        userRepository = mock(UserRepository.class);
        clubAuthorizationService = mock(ClubAuthorizationService.class);
        attendanceService = new AttendanceService(
                registrationRepository,
                attendanceLogRepository,
                userRepository,
                clubAuthorizationService
        );
    }

    @Test
    void checkIn_whenAlreadyCheckedIn_throwsConflict() {
        Registration registration = createRegistration(RegistrationStatus.CHECKED_IN);
        when(registrationRepository.findByQrToken("token-1")).thenReturn(Optional.of(registration));

        AppException exception = assertThrows(
                AppException.class,
                () -> attendanceService.checkIn(1L, "token-1")
        );
        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    @Test
    void checkIn_success_updatesStatusAndSavesLog() {
        Registration registration = createRegistration(RegistrationStatus.REGISTERED);
        User checker = new User("dev-admin", "admin");
        checker.updateRealName("관리자");

        when(registrationRepository.findByQrToken("token-1")).thenReturn(Optional.of(registration));
        when(userRepository.findById(1L)).thenReturn(Optional.of(checker));

        var result = attendanceService.checkIn(1L, "token-1");

        assertEquals(RegistrationStatus.CHECKED_IN, registration.getStatus());
        assertEquals(registration.getId(), result.registrationId());
        assertEquals("관리자", result.username());
        verify(attendanceLogRepository).save(any());
        verify(clubAuthorizationService).requireAdminOrOwner(any(), eq(1L));
    }

    @Test
    void checkIn_withoutQrToken_throwsBadRequest() {
        AppException exception = assertThrows(
                AppException.class,
                () -> attendanceService.checkIn(1L, " ")
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    private Registration createRegistration(RegistrationStatus status) {
        Club club = new Club("UMC", "desc", "guild-1", null);
        Event event = new Event(
                club,
                "OT",
                LocalDateTime.parse("2026-03-10T19:00:00"),
                LocalDateTime.parse("2026-03-10T19:00:00"),
                null,
                true
        );
        User user = new User("dev-user", "member");
        Registration registration = new Registration(event, user, "token-1", status);
        try {
            var idField = Registration.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(registration, 200L);
        } catch (Exception ignored) {
        }
        return registration;
    }
}
