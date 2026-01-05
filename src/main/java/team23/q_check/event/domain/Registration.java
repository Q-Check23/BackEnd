package team23.q_check.event.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import team23.q_check.identity.domain.User;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "registrations", uniqueConstraints = {
        @UniqueConstraint(
                name = "uq_registrations_event_user",
                columnNames = {"event_id", "user_id"}
        ),
        @UniqueConstraint(name = "uq_registrations_qr_token", columnNames = {"qr_token"})
})
public class Registration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "qr_token", nullable = false, length = 100, unique = true)
    private String qrToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RegistrationStatus status;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt = LocalDateTime.now(ZoneOffset.UTC);

    @Column(name = "register_fee_paid", nullable = false)
    private boolean registerFeePaid = false;

}
