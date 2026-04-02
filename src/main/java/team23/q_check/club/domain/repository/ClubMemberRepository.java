package team23.q_check.club.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team23.q_check.club.domain.model.ClubMember;

import java.util.List;
import java.util.Optional;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {
    Optional<ClubMember> findByClub_IdAndUser_Id(Long clubId, Long userId);

    List<ClubMember> findAllByUser_Id(Long userId);

    List<ClubMember> findAllByClub_Id(Long clubId);

    boolean existsByClub_IdAndUser_Id(Long clubId, Long userId);

    Optional<ClubMember> findByIdAndClub_Id(Long memberId, Long clubId);
}
