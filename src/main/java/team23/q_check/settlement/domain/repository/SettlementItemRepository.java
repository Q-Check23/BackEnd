package team23.q_check.settlement.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team23.q_check.settlement.domain.model.SettlementItem;

import java.util.List;
import java.util.Optional;

public interface SettlementItemRepository extends JpaRepository<SettlementItem, Long> {

    List<SettlementItem> findAllBySettlement_Id(Long settlementId);

    Optional<SettlementItem> findByIdAndUser_Id(Long itemId, Long userId);
}
