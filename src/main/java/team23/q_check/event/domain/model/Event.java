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

    protected Event() {
    }

    public Event(
            Club club,
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String location,
            Boolean isActive
    ) {
        this.club = club;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.isActive = isActive;
    }

    public Long getId() {
        return id;
    }

    public Club getClub() {
        return club;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public String getLocation() {
        return location;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void update(String title, LocalDateTime startTime, String location, Boolean isActive) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (startTime != null) {
            this.startTime = startTime;
            this.endTime = startTime;
        }
        if (location != null) {
            this.location = location;
        }
        if (isActive != null) {
            this.isActive = isActive;
        }
    }
}
