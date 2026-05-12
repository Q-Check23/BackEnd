package team23.q_check.notice.domain.model;

import jakarta.persistence.*;
import team23.q_check.club.domain.model.Club;
import team23.q_check.identity.domain.model.User;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "notices")
public class Notice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 255)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

    protected Notice() {
    }

    public Notice(Club club, User author, String title, String content) {
        this.club = club;
        this.author = author;
        this.title = title;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public Club getClub() {
        return club;
    }

    public User getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void updateContent(String title, String content) {
        if (title != null && !title.isBlank()) {
            this.title = title.trim();
        }
        if (content != null && !content.isBlank()) {
            this.content = content.trim();
        }
    }
}
