package team23.q_check.club.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team23.q_check.club.domain.model.Club;

import java.util.Optional;

public interface ClubRepository extends JpaRepository<Club, Long> {
    boolean existsByDiscordGuildId(String discordGuildId);

    boolean existsByInviteCode(String inviteCode);

    Optional<Club> findByInviteCode(String inviteCode);
}
