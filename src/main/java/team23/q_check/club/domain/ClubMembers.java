package team23.q_check.club.domain;

import jakarta.persistence.*;
import team23.q_check.identity.domain.User;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "club_members", uniqueConstraints = {
        @UniqueConstraint(
                name = "uq_club_members_club_user",
                columnNames = {"club_id, user_id"}
        )
})
public class ClubMembers {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClubRole role;

    @Column(nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now(ZoneOffset.UTC);
}
