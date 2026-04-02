package team23.q_check.identity.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team23.q_check.identity.domain.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
}
