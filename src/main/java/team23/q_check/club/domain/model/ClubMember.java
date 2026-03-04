package team23.q_check.club.domain.model;

import jakarta.persistence.*;
import team23.q_check.identity.domain.model.User;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "club_members", uniqueConstraints = {
        @UniqueConstraint(
                name = "uq_club_members_club_user",
                columnNames = {"club_id", "user_id"}
        )
})
public class ClubMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClubRole role;

    @Column(nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now(ZoneOffset.UTC);

    protected ClubMember() {
    }

    public ClubMember(Club club, User user, ClubRole role) {
        this.club = club;
        this.user = user;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public Club getClub() {
        return club;
    }

    public User getUser() {
        return user;
    }

    public ClubRole getRole() {
        return role;
    }

    public void changeRole(ClubRole role) {
        this.role = role;
    }
}
