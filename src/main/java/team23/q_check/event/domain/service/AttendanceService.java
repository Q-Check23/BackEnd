package team23.q_check.event.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team23.q_check.club.domain.service.ClubAuthorizationService;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.event.domain.model.AttendanceLog;
import team23.q_check.event.domain.model.AttendanceMethod;
import team23.q_check.event.domain.model.Registration;
import team23.q_check.event.domain.model.RegistrationStatus;
import team23.q_check.event.dto.CheckInResponseDto;
import team23.q_check.event.domain.repository.AttendanceLogRepository;
import team23.q_check.event.domain.repository.RegistrationRepository;
import team23.q_check.identity.domain.model.User;
import team23.q_check.identity.domain.repository.UserRepository;

import java.time.LocalDateTime;

@Service
public class AttendanceService {

    private final RegistrationRepository registrationRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    private final UserRepository userRepository;
    private final ClubAuthorizationService clubAuthorizationService;

    public AttendanceService(
            RegistrationRepository registrationRepository,
            AttendanceLogRepository attendanceLogRepository,
            UserRepository userRepository,
            ClubAuthorizationService clubAuthorizationService
    ) {
        this.registrationRepository = registrationRepository;
        this.attendanceLogRepository = attendanceLogRepository;
        this.userRepository = userRepository;
        this.clubAuthorizationService = clubAuthorizationService;
    }

    @Transactional
    public CheckInResponseDto checkIn(Long currentUserId, String qrToken) {
        if (qrToken == null || qrToken.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "qrToken is required");
        }

        Registration registration = registrationRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Registration not found for qrToken"));

        return performCheckIn(currentUserId, registration, AttendanceMethod.QR);
    }

    /**
     * 참가자가 행사 QR을 스캔하여 본인 체크인을 처리한다.
     */
    @Transactional
    public CheckInResponseDto selfCheckIn(Long currentUserId, Long eventId) {
        if (eventId == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "eventId is required");
        }

        Registration registration = registrationRepository.findByEvent_IdAndUser_Id(eventId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Registration not found for this event"));

        if (registration.getStatus() == RegistrationStatus.CHECKED_IN) {
            throw new AppException(ErrorCode.CONFLICT, "Already checked in");
        }

        if (registration.getStatus() == RegistrationStatus.CANCELED) {
            throw new AppException(ErrorCode.CONFLICT, "Registration has been canceled");
        }

        User attendee = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found: " + currentUserId));

        LocalDateTime now = LocalDateTime.now();
        registration.updateStatus(RegistrationStatus.CHECKED_IN);

        AttendanceLog attendanceLog = new AttendanceLog(
                registration.getEvent(),
                registration,
                attendee,
                now,
                AttendanceMethod.QR
        );
        attendanceLogRepository.save(attendanceLog);

        String displayName = attendee.getRealName() != null && !attendee.getRealName().isBlank()
                ? attendee.getRealName()
                : attendee.getUsername();

        return new CheckInResponseDto(registration.getId(), now.toString(), displayName);
    }

    /**
     * 관리자가 registrationId로 직접 체크인을 처리한다.
     * QR 인식 실패·지각 등 운영 상황에서 사용한다.
     */
    @Transactional
    public CheckInResponseDto manualCheckIn(Long currentUserId, Long registrationId) {
        if (registrationId == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "registrationId is required");
        }

        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Registration not found: " + registrationId));

        return performCheckIn(currentUserId, registration, AttendanceMethod.MANUAL);
    }

    /**
     * 권한 검증·상태 전이·로그 생성 공통 흐름.
     * QR과 Manual이 공유한다.
     */
    private CheckInResponseDto performCheckIn(Long currentUserId, Registration registration, AttendanceMethod method) {
        clubAuthorizationService.requireAdminOrOwner(registration.getEvent().getClub().getId(), currentUserId);

        if (registration.getStatus() == RegistrationStatus.CHECKED_IN) {
            throw new AppException(ErrorCode.CONFLICT, "Already checked in");
        }

        User checker = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found: " + currentUserId));

        LocalDateTime now = LocalDateTime.now();
        registration.updateStatus(RegistrationStatus.CHECKED_IN);

        AttendanceLog attendanceLog = new AttendanceLog(
                registration.getEvent(),
                registration,
                checker,
                now,
                method
        );
        attendanceLogRepository.save(attendanceLog);

        User attendee = registration.getUser();
        String displayName = attendee.getRealName() != null && !attendee.getRealName().isBlank()
                ? attendee.getRealName()
                : attendee.getUsername();

        return new CheckInResponseDto(registration.getId(), now.toString(), displayName);
    }
}
