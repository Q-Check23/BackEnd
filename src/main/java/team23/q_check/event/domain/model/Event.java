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

    @Column(name = "discord_channel_id", nullable = true, length = 50)
    private String discordChannelId;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal registerFee = BigDecimal.ZERO;

    @Column(name = "collect_registration_info", nullable = false)
    private Boolean collectRegistrationInfo = true;

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
        this(club, title, null, startTime, endTime, location, null, isActive, BigDecimal.ZERO, true);
    }

    public Event(
            Club club,
            String title,
            String description,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String location,
            String discordChannelId,
            Boolean isActive,
            BigDecimal registerFee
    ) {
        this(club, title, description, startTime, endTime, location, discordChannelId, isActive, registerFee, true);
    }

    public Event(
            Club club,
            String title,
            String description,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String location,
            String discordChannelId,
            Boolean isActive,
            BigDecimal registerFee,
            Boolean collectRegistrationInfo
    ) {
        this.club = club;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.discordChannelId = discordChannelId;
        this.isActive = isActive == null ? Boolean.TRUE : isActive;
        this.registerFee = registerFee == null ? BigDecimal.ZERO : registerFee;
        this.collectRegistrationInfo = collectRegistrationInfo == null ? Boolean.TRUE : collectRegistrationInfo;
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

    public String getDescription() {
        return description;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getLocation() {
        return location;
    }

    public String getDiscordChannelId() {
        return discordChannelId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public BigDecimal getRegisterFee() {
        return registerFee;
    }

    public Boolean getCollectRegistrationInfo() {
        return collectRegistrationInfo;
    }

    public void update(
            String title,
            String description,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String location,
            String discordChannelId,
            BigDecimal registerFee,
            Boolean isActive
    ) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (startTime != null) {
            this.startTime = startTime;
        }
        if (endTime != null) {
            this.endTime = endTime;
        }
        if (location != null) {
            this.location = location;
        }
        if (discordChannelId != null) {
            this.discordChannelId = discordChannelId;
        }
        if (registerFee != null) {
            this.registerFee = registerFee;
        }
        if (isActive != null) {
            this.isActive = isActive;
        }
    }
}
