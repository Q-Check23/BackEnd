package team23.q_check.event.domain.model;

import jakarta.persistence.*;
import team23.q_check.club.domain.model.Club;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id")
    private Club club;

    @Column(nullable = false)
    private String title;

    @Column(nullable = true)
    private String description;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = true, length = 100)
    private String location;

    @Column(nullable = true, length = 50)
    private String discord_channel_id;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal registerFee = BigDecimal.ZERO;
}
