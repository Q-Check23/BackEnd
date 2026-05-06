package team23.q_check.settlement.domain.model;

import jakarta.persistence.*;
import team23.q_check.event.domain.model.Event;
import team23.q_check.identity.domain.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "settlements")
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "receipt_image_url", length = 255)
    private String receiptImageUrl;

    @Column(name = "ocr_data", columnDefinition = "json")
    private String ocrData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

    protected Settlement() {
    }

    public Settlement(
            Event event,
            User createdBy,
            String title,
            BigDecimal totalAmount,
            String receiptImageUrl
    ) {
        this.event = event;
        this.createdBy = createdBy;
        this.title = title;
        this.totalAmount = totalAmount;
        this.receiptImageUrl = receiptImageUrl;
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getReceiptImageUrl() {
        return receiptImageUrl;
    }

    public String getOcrData() {
        return ocrData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
