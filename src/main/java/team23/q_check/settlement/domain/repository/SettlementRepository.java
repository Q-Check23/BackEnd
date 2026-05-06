package team23.q_check.settlement.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team23.q_check.settlement.domain.model.Settlement;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findAllByEvent_IdOrderByCreatedAtDesc(Long eventId);
}
