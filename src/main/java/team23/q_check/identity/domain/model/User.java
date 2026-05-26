package team23.q_check.identity.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String discordId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = true)
    private String realName;

    @Column(unique = true)
    private String email;

    @Column(nullable = true)
    private String phone;

    @Column(length = 512)
    private String refreshToken;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected User() {
    }

    /** Discord OAuth2 회원가입 */
    public User(String discordId, String email, String username, String realName) {
        this.discordId = discordId;
        this.email = email;
        this.username = username;
        this.realName = realName;
    }

    public Long getId() {
        return id;
    }

    public String getDiscordId() {
        return discordId;
    }

    public String getUsername() {
        return username;
    }

    public String getRealName() {
        return realName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void updateRealName(String realName) {
        this.realName = realName;
    }

    public void updateUsername(String username) {
        if (username != null && !username.isBlank()) {
            this.username = username;
        }
    }

    public void updatePhone(String phone) {
        this.phone = phone;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void markDeleted() {
        this.deletedAt = LocalDateTime.now();
        this.refreshToken = null;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
