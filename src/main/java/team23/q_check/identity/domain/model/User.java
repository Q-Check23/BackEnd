package team23.q_check.identity.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
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

    @Column(length = 512)
    private String refreshToken;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected User() {
    }

    /** 기존 코드 호환용 */
    public User(String discordId, String username) {
        this.discordId = discordId;
        this.username = username;
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

    public String getRefreshToken() {
        return refreshToken;
    }

    public void updateRealName(String realName) {
        this.realName = realName;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
