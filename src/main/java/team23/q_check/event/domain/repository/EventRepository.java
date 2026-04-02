package team23.q_check.event.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team23.q_check.event.domain.model.Event;

public interface EventRepository extends JpaRepository<Event, Long> {
}
