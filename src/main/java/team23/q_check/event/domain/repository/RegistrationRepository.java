package team23.q_check.event.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team23.q_check.event.domain.model.Registration;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    boolean existsByEvent_IdAndUser_Id(Long eventId, Long userId);

    Optional<Registration> findByEvent_IdAndUser_Id(Long eventId, Long userId);

    List<Registration> findAllByEvent_Id(Long eventId);

    Optional<Registration> findByQrToken(String qrToken);
}
