package team23.q_check.event.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team23.q_check.club.service.ClubAuthorizationService;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.event.domain.model.AttendanceLog;
import team23.q_check.event.domain.model.AttendanceMethod;
import team23.q_check.event.domain.model.Registration;
import team23.q_check.event.domain.model.RegistrationStatus;
import team23.q_check.event.dto.CheckInResponseDto;
import team23.q_check.event.repository.AttendanceLogRepository;
import team23.q_check.event.repository.RegistrationRepository;
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
                AttendanceMethod.QR
        );
        attendanceLogRepository.save(attendanceLog);

        String displayName = checker.getRealName() != null && !checker.getRealName().isBlank()
                ? checker.getRealName()
                : checker.getUsername();

        return new CheckInResponseDto(registration.getId(), now.toString(), displayName);
    }
}
