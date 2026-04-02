package team23.q_check.club.domain.service;

import org.springframework.stereotype.Service;
import team23.q_check.club.domain.model.ClubMember;
import team23.q_check.club.domain.model.ClubRole;
import team23.q_check.club.domain.repository.ClubMemberRepository;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;

@Service
public class ClubAuthorizationService {

    private final ClubMemberRepository clubMemberRepository;

    public ClubAuthorizationService(ClubMemberRepository clubMemberRepository) {
        this.clubMemberRepository = clubMemberRepository;
    }

    public ClubMember requireMembership(Long clubId, Long userId) {
        return clubMemberRepository.findByClub_IdAndUser_Id(clubId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN, "Not a member of this club"));
    }

    public ClubMember requireAdminOrOwner(Long clubId, Long userId) {
        ClubMember membership = requireMembership(clubId, userId);
        if (membership.getRole() != ClubRole.OWNER && membership.getRole() != ClubRole.ADMIN) {
            throw new AppException(ErrorCode.FORBIDDEN, "Only OWNER or ADMIN can perform this action");
        }
        return membership;
    }
}
