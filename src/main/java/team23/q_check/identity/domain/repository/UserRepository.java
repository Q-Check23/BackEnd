package team23.q_check.identity.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team23.q_check.identity.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);

    /** 아이디 부분일치 검색 (대소문자 무시). 검색 폭주를 막기 위해 상한을 둔다. */
    List<User> findTop20ByUsernameContainingIgnoreCaseOrderByUsernameAsc(String fragment);
}
