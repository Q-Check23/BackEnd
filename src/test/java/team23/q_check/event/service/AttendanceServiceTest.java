package team23.q_check.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import team23.q_check.club.domain.model.Club;
import team23.q_check.club.domain.service.ClubAuthorizationService;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import org.mockito.ArgumentCaptor;
import team23.q_check.event.domain.model.AttendanceLog;
import team23.q_check.event.domain.model.AttendanceMethod;
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
        User checker = new User("dev-admin", null, "admin", null);
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

    @Test
    void manualCheckIn_success_updatesStatusAndSavesLogWithMethodManual() {
        Registration registration = createRegistration(RegistrationStatus.REGISTERED);
        User checker = new User("dev-admin", null, "admin", null);
        checker.updateRealName("관리자");

        when(registrationRepository.findById(200L)).thenReturn(Optional.of(registration));
        when(userRepository.findById(1L)).thenReturn(Optional.of(checker));

        var result = attendanceService.manualCheckIn(1L, 200L);

        assertEquals(RegistrationStatus.CHECKED_IN, registration.getStatus());
        assertEquals(200L, result.registrationId());
        assertEquals("관리자", result.username());

        ArgumentCaptor<AttendanceLog> captor = ArgumentCaptor.forClass(AttendanceLog.class);
        verify(attendanceLogRepository).save(captor.capture());
        assertEquals(AttendanceMethod.MANUAL, captor.getValue().getMethod());
    }

    @Test
    void manualCheckIn_whenAlreadyCheckedIn_throwsConflict() {
        Registration registration = createRegistration(RegistrationStatus.CHECKED_IN);
        when(registrationRepository.findById(200L)).thenReturn(Optional.of(registration));

        AppException exception = assertThrows(
                AppException.class,
                () -> attendanceService.manualCheckIn(1L, 200L)
        );
        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    @Test
    void manualCheckIn_whenRegistrationIdMissing_throwsBadRequest() {
        AppException exception = assertThrows(
                AppException.class,
                () -> attendanceService.manualCheckIn(1L, null)
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void manualCheckIn_whenRegistrationNotFound_throwsNotFound() {
        when(registrationRepository.findById(999L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> attendanceService.manualCheckIn(1L, 999L)
        );
        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void manualCheckIn_whenNotAdmin_throwsForbidden() {
        Registration registration = createRegistration(RegistrationStatus.REGISTERED);
        when(registrationRepository.findById(200L)).thenReturn(Optional.of(registration));
        doThrow(new AppException(ErrorCode.FORBIDDEN, "forbidden"))
                .when(clubAuthorizationService).requireAdminOrOwner(any(), eq(99L));

        AppException exception = assertThrows(
                AppException.class,
                () -> attendanceService.manualCheckIn(99L, 200L)
        );
        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        verify(attendanceLogRepository, never()).save(any());
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
        User user = new User("dev-user", null, "member", null);
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
