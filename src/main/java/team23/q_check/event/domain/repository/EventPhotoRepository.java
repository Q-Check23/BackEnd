package team23.q_check.event.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team23.q_check.event.domain.model.EventPhoto;

import java.util.List;
import java.util.Optional;

public interface EventPhotoRepository extends JpaRepository<EventPhoto, Long> {

    List<EventPhoto> findAllByEvent_IdOrderByCreatedAtDesc(Long eventId);

    Optional<EventPhoto> findByIdAndEvent_Id(Long photoId, Long eventId);

    void deleteAllByEvent_Id(Long eventId);
}
