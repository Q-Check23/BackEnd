package team23.q_check.event.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team23.q_check.event.domain.model.Registration;
import team23.q_check.event.domain.model.RegistrationStatus;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    boolean existsByEvent_IdAndUser_Id(Long eventId, Long userId);

    Optional<Registration> findByEvent_IdAndUser_Id(Long eventId, Long userId);

    List<Registration> findAllByEvent_Id(Long eventId);

    Optional<Registration> findByQrToken(String qrToken);

    @Query("SELECT r.event.id FROM Registration r WHERE r.user.id = :userId AND r.status IN :statuses")
    List<Long> findEventIdsByUserIdAndStatuses(@Param("userId") Long userId,
                                               @Param("statuses") List<RegistrationStatus> statuses);

    boolean existsByEvent_IdAndStatusIn(Long eventId, List<RegistrationStatus> statuses);

    void deleteAllByEvent_Id(Long eventId);

    long countByUser_IdAndStatus(Long userId, RegistrationStatus status);

    long countByEvent_Id(Long eventId);

    long countByEvent_IdAndStatus(Long eventId, RegistrationStatus status);

    @Query("""
            SELECT COUNT(r) FROM Registration r
            WHERE r.user.id = :userId
              AND r.status = team23.q_check.event.domain.model.RegistrationStatus.REGISTERED
              AND r.event.startTime > CURRENT_TIMESTAMP
            """)
    long countUpcomingRegisteredEvents(@Param("userId") Long userId);
}
