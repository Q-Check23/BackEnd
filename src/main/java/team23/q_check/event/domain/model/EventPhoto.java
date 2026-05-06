package team23.q_check.event.domain.model;

import jakarta.persistence.*;
import team23.q_check.identity.domain.model.User;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "event_photos")
public class EventPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_user_id", nullable = false)
    private User uploader;

    @Column(nullable = false, length = 255)
    private String photoUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

    protected EventPhoto() {
    }

    public EventPhoto(Event event, User uploader, String photoUrl) {
        this.event = event;
        this.uploader = uploader;
        this.photoUrl = photoUrl;
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public User getUploader() {
        return uploader;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
