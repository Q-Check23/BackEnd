package team23.q_check.club.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team23.q_check.club.domain.model.Club;

public interface ClubRepository extends JpaRepository<Club, Long> {
    boolean existsByDiscordGuildId(String discordGuildId);
}
