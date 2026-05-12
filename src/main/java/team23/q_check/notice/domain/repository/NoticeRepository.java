package team23.q_check.notice.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team23.q_check.notice.domain.model.Notice;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByClub_IdOrderByCreatedAtDesc(Long clubId);

    long countByClub_Id(Long clubId);
}
