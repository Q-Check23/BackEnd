package team23.q_check.identity.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String discordId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = true)
    private String realName;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
