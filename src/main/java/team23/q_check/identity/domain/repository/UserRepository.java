package team23.q_check.identity.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team23.q_check.identity.domain.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
