package team23.q_check.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team23.q_check.event.domain.model.AttendanceLog;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {
}
