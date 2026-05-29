package team23.q_check.event.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team23.q_check.event.domain.model.AttendanceLog;

import java.util.List;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {

    void deleteAllByEvent_Id(Long eventId);

    List<AttendanceLog> findAllByEvent_Id(Long eventId);
}
