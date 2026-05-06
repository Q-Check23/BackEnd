package team23.q_check.settlement.domain.model;

import jakarta.persistence.*;
import team23.q_check.identity.domain.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_items", uniqueConstraints = {
        @UniqueConstraint(
                name = "uq_settlement_items_settlement_user",
                columnNames = {"settlement_id", "user_id"}
        )
})
public class SettlementItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SettlementItemStatus status = SettlementItemStatus.UNPAID;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_reminded_at")
    private LocalDateTime lastRemindedAt;

    @Column(name = "remind_count", nullable = false)
    private long remindCount = 0L;

    protected SettlementItem() {
    }

    public SettlementItem(Settlement settlement, User user, BigDecimal amount) {
        this.settlement = settlement;
        this.user = user;
        this.amount = amount;
        this.status = SettlementItemStatus.UNPAID;
    }

    public Long getId() {
        return id;
    }

    public Settlement getSettlement() {
        return settlement;
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public SettlementItemStatus getStatus() {
        return status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getLastRemindedAt() {
        return lastRemindedAt;
    }

    public long getRemindCount() {
        return remindCount;
    }

    public void markAsPending() {
        this.status = SettlementItemStatus.PENDING;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsCompleted() {
        this.status = SettlementItemStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void recordReminder() {
        this.lastRemindedAt = LocalDateTime.now();
        this.remindCount += 1L;
    }
}
