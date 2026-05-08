package team23.q_check.club.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "clubs", uniqueConstraints = {
        @UniqueConstraint(
                name = "uq_clubs_discord_guild_id",
                columnNames = {"discord_guild_id"}
        ),
        @UniqueConstraint(
                name = "uq_clubs_invite_code",
                columnNames = {"invite_code"}
        )
})
public class Club {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = true, length = 255)
    private String description;

    @Column(nullable = false, length = 50)
    private String discordGuildId;

    @Column(nullable = true, length = 255)
    private String coverImageUrl;

    @Column(name = "invite_code", nullable = false, length = 16)
    private String inviteCode;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

    protected Club() {
    }

    public Club(String name, String description, String discordGuildId, String coverImageUrl, String inviteCode) {
        this.name = name;
        this.description = description;
        this.discordGuildId = discordGuildId;
        this.coverImageUrl = coverImageUrl;
        this.inviteCode = inviteCode;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDiscordGuildId() {
        return discordGuildId;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void updateInfo(String name, String description, String coverImageUrl) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        if (description != null) {
            this.description = description;
        }
        if (coverImageUrl != null) {
            this.coverImageUrl = coverImageUrl;
        }
    }
}
