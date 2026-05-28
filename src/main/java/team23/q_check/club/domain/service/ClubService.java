package team23.q_check.club.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team23.q_check.club.domain.model.Club;
import team23.q_check.club.domain.model.ClubMember;
import team23.q_check.club.domain.model.ClubRole;
import team23.q_check.club.dto.AddClubMemberRequestDto;
import team23.q_check.club.dto.ClubDetailResponseDto;
import team23.q_check.club.dto.ClubMemberResponseDto;
import team23.q_check.club.dto.CreateClubRequestDto;
import team23.q_check.club.dto.JoinClubRequestDto;
import team23.q_check.club.dto.MyClubResponseDto;
import team23.q_check.club.dto.UpdateClubMemberRoleRequestDto;
import team23.q_check.club.dto.UpdateClubRequestDto;
import team23.q_check.club.domain.repository.ClubMemberRepository;
import team23.q_check.club.domain.repository.ClubRepository;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.event.domain.model.Event;
import team23.q_check.event.domain.repository.EventRepository;
import team23.q_check.identity.domain.model.User;
import team23.q_check.identity.domain.repository.UserRepository;
import team23.q_check.notice.domain.repository.NoticeRepository;

import java.security.SecureRandom;
import java.util.List;

@Service
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;
    private final ClubAuthorizationService clubAuthorizationService;
    private final EventRepository eventRepository;
    private final NoticeRepository noticeRepository;

    public ClubService(
            ClubRepository clubRepository,
            ClubMemberRepository clubMemberRepository,
            UserRepository userRepository,
            ClubAuthorizationService clubAuthorizationService,
            EventRepository eventRepository,
            NoticeRepository noticeRepository
    ) {
        this.clubRepository = clubRepository;
        this.clubMemberRepository = clubMemberRepository;
        this.userRepository = userRepository;
        this.clubAuthorizationService = clubAuthorizationService;
        this.eventRepository = eventRepository;
        this.noticeRepository = noticeRepository;
    }

    @Transactional
    public MyClubResponseDto createClub(Long currentUserId, CreateClubRequestDto request) {
        validateCreateRequest(request);

        String guildId = (request.discordGuildId() != null && !request.discordGuildId().isBlank())
                ? request.discordGuildId().trim()
                : null;

        if (guildId != null && clubRepository.existsByDiscordGuildId(guildId)) {
            throw new AppException(ErrorCode.CONFLICT, "Club already exists with discordGuildId: " + guildId);
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found: " + currentUserId));

        Club club = new Club(
                request.name().trim(),
                request.description(),
                guildId,
                request.coverImageUrl(),
                generateUniqueInviteCode()
        );
        Club savedClub = clubRepository.save(club);
        ClubMember ownerMembership = new ClubMember(savedClub, currentUser, ClubRole.OWNER);
        clubMemberRepository.save(ownerMembership);

        return new MyClubResponseDto(
                savedClub.getId(),
                savedClub.getName(),
                savedClub.getDescription(),
                ClubRole.OWNER
        );
    }

    @Transactional
    public MyClubResponseDto joinClub(Long currentUserId, JoinClubRequestDto request) {
        if (request == null || request.inviteCode() == null || request.inviteCode().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "inviteCode is required");
        }
        Club club = clubRepository.findByInviteCode(request.inviteCode().trim())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Invalid invite code"));

        if (clubMemberRepository.existsByClub_IdAndUser_Id(club.getId(), currentUserId)) {
            throw new AppException(ErrorCode.CONFLICT, "Already a member of this club");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found: " + currentUserId));

        clubMemberRepository.save(new ClubMember(club, currentUser, ClubRole.MEMBER));
        return new MyClubResponseDto(club.getId(), club.getName(), club.getDescription(), ClubRole.MEMBER);
    }

    @Transactional
    public MyClubResponseDto joinClubViaEvent(Long currentUserId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Event not found: " + eventId));
        Club club = event.getClub();

        ClubMember existing = clubMemberRepository
                .findByClub_IdAndUser_Id(club.getId(), currentUserId)
                .orElse(null);
        if (existing != null) {
            return new MyClubResponseDto(club.getId(), club.getName(), club.getDescription(), existing.getRole());
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found: " + currentUserId));
        clubMemberRepository.save(new ClubMember(club, currentUser, ClubRole.MEMBER));
        return new MyClubResponseDto(club.getId(), club.getName(), club.getDescription(), ClubRole.MEMBER);
    }

    @Transactional
    public void deleteClub(Long currentUserId, Long clubId) {
        ClubMember membership = clubAuthorizationService.requireMembership(clubId, currentUserId);
        if (membership.getRole() != ClubRole.OWNER) {
            throw new AppException(ErrorCode.FORBIDDEN, "Only OWNER can delete the club");
        }
        if (eventRepository.countByClub_Id(clubId) > 0) {
            throw new AppException(ErrorCode.CONFLICT, "Delete all events before deleting the club");
        }
        if (noticeRepository.countByClub_Id(clubId) > 0) {
            throw new AppException(ErrorCode.CONFLICT, "Delete all notices before deleting the club");
        }
        if (clubMemberRepository.countByClub_Id(clubId) > 1) {
            throw new AppException(ErrorCode.CONFLICT, "Remove all other members before deleting the club");
        }
        clubMemberRepository.delete(membership);
        clubRepository.deleteById(clubId);
    }

    @Transactional
    public ClubDetailResponseDto updateClub(Long currentUserId, Long clubId, UpdateClubRequestDto request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Request body is required");
        }
        ClubMember membership = clubAuthorizationService.requireAdminOrOwner(clubId, currentUserId);
        Club club = membership.getClub();

        if (request.discordGuildId() != null && !request.discordGuildId().isBlank()) {
            String newGuildId = request.discordGuildId().trim();
            if (!newGuildId.equals(club.getDiscordGuildId())
                    && clubRepository.existsByDiscordGuildId(newGuildId)) {
                throw new AppException(ErrorCode.CONFLICT,
                        "Club already exists with discordGuildId: " + newGuildId);
            }
        }

        club.updateInfo(request.name(), request.description(), request.coverImageUrl(), request.discordGuildId());
        long memberCount = clubMemberRepository.countByClub_Id(clubId);
        return new ClubDetailResponseDto(
                club.getId(),
                club.getName(),
                club.getDescription(),
                memberCount,
                membership.getRole(),
                club.getDiscordGuildId()
        );
    }

    @Transactional(readOnly = true)
    public ClubDetailResponseDto getClubDetail(Long currentUserId, Long clubId) {
        ClubMember membership = clubAuthorizationService.requireMembership(clubId, currentUserId);
        Club club = membership.getClub();
        long memberCount = clubMemberRepository.countByClub_Id(clubId);
        return new ClubDetailResponseDto(
                club.getId(),
                club.getName(),
                club.getDescription(),
                memberCount,
                membership.getRole(),
                club.getDiscordGuildId()
        );
    }

    @Transactional(readOnly = true)
    public String getInviteCode(Long currentUserId, Long clubId) {
        ClubMember membership = clubAuthorizationService.requireAdminOrOwner(clubId, currentUserId);
        return membership.getClub().getInviteCode();
    }

    @Transactional(readOnly = true)
    public List<MyClubResponseDto> getMyClubs(Long currentUserId) {
        return clubMemberRepository.findAllByUser_Id(currentUserId).stream()
                .map(member -> new MyClubResponseDto(
                        member.getClub().getId(),
                        member.getClub().getName(),
                        member.getClub().getDescription(),
                        member.getRole()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClubMemberResponseDto> getClubMembers(Long currentUserId, Long clubId) {
        clubAuthorizationService.requireMembership(clubId, currentUserId);
        return clubMemberRepository.findAllByClub_Id(clubId).stream()
                .map(member -> new ClubMemberResponseDto(
                        member.getId(),
                        member.getUser().getId(),
                        member.getUser().getUsername(),
                        member.getRole()
                ))
                .toList();
    }

    @Transactional
    public ClubMemberResponseDto addClubMember(Long currentUserId, Long clubId, AddClubMemberRequestDto request) {
        ClubMember operatorMembership = clubAuthorizationService.requireAdminOrOwner(clubId, currentUserId);
        if (request == null || request.userId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "userId is required");
        }

        if (clubMemberRepository.existsByClub_IdAndUser_Id(clubId, request.userId())) {
            throw new AppException(ErrorCode.CONFLICT, "User is already a club member");
        }

        User targetUser = userRepository.findById(request.userId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found: " + request.userId()));

        ClubMember member = new ClubMember(operatorMembership.getClub(), targetUser, ClubRole.MEMBER);
        ClubMember savedMember = clubMemberRepository.save(member);
        return new ClubMemberResponseDto(
                savedMember.getId(),
                savedMember.getUser().getId(),
                savedMember.getUser().getUsername(),
                savedMember.getRole()
        );
    }

    /**
     * 본인이 클럽에서 탈퇴한다. 마지막 OWNER이면 거부 (먼저 위임 필요).
     */
    @Transactional
    public void leaveClub(Long currentUserId, Long clubId) {
        ClubMember membership = clubAuthorizationService.requireMembership(clubId, currentUserId);
        if (membership.getRole() == ClubRole.OWNER && isLastOwner(clubId)) {
            throw new AppException(ErrorCode.CONFLICT,
                    "Last OWNER cannot leave the club. Transfer ownership first.");
        }
        clubMemberRepository.delete(membership);
    }

    /**
     * 관리자(OWNER/ADMIN)가 다른 멤버를 클럽에서 제거한다.
     * - ADMIN은 OWNER를 제거할 수 없다
     * - 마지막 OWNER는 제거할 수 없다 (사실상 OWNER끼리만 가능한 상황에서만 차단)
     * - 본인을 이 엔드포인트로 제거하는 것은 거부 (탈퇴는 /me 엔드포인트 사용)
     */
    @Transactional
    public void removeClubMember(Long currentUserId, Long clubId, Long memberId) {
        ClubMember operator = clubAuthorizationService.requireAdminOrOwner(clubId, currentUserId);
        ClubMember target = clubMemberRepository.findByIdAndClub_Id(memberId, clubId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Club member not found: " + memberId));

        if (target.getId().equals(operator.getId())) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Use the leave endpoint to remove yourself");
        }
        if (operator.getRole() == ClubRole.ADMIN && target.getRole() == ClubRole.OWNER) {
            throw new AppException(ErrorCode.FORBIDDEN, "ADMIN cannot remove an OWNER");
        }
        if (target.getRole() == ClubRole.OWNER && isLastOwner(clubId)) {
            throw new AppException(ErrorCode.CONFLICT,
                    "Last OWNER cannot be removed. Transfer ownership first.");
        }

        clubMemberRepository.delete(target);
    }

    private boolean isLastOwner(Long clubId) {
        return clubMemberRepository.countByClub_IdAndRole(clubId, ClubRole.OWNER) <= 1L;
    }

    @Transactional
    public ClubMemberResponseDto updateClubMemberRole(
            Long currentUserId,
            Long clubId,
            Long memberId,
            UpdateClubMemberRoleRequestDto request
    ) {
        ClubMember operatorMembership = clubAuthorizationService.requireAdminOrOwner(clubId, currentUserId);
        ClubMember targetMember = clubMemberRepository.findByIdAndClub_Id(memberId, clubId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Club member not found: " + memberId));

        ClubRole nextRole = parseRole(request);

        if (operatorMembership.getRole() == ClubRole.ADMIN && nextRole == ClubRole.OWNER) {
            throw new AppException(ErrorCode.FORBIDDEN, "ADMIN cannot assign OWNER role");
        }
        if (operatorMembership.getRole() == ClubRole.ADMIN && targetMember.getRole() == ClubRole.OWNER) {
            throw new AppException(ErrorCode.FORBIDDEN, "ADMIN cannot modify OWNER role");
        }

        targetMember.changeRole(nextRole);
        return new ClubMemberResponseDto(
                targetMember.getId(),
                targetMember.getUser().getId(),
                targetMember.getUser().getUsername(),
                targetMember.getRole()
        );
    }

    private void validateCreateRequest(CreateClubRequestDto request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Request body is required");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "name is required");
        }
    }

    private static final char[] INVITE_CODE_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int INVITE_CODE_LENGTH = 8;
    private static final SecureRandom INVITE_CODE_RANDOM = new SecureRandom();

    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            char[] buf = new char[INVITE_CODE_LENGTH];
            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                buf[i] = INVITE_CODE_ALPHABET[INVITE_CODE_RANDOM.nextInt(INVITE_CODE_ALPHABET.length)];
            }
            String code = new String(buf);
            if (!clubRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new AppException(ErrorCode.INTERNAL_ERROR, "Failed to allocate a unique invite code");
    }

    private ClubRole parseRole(UpdateClubMemberRoleRequestDto request) {
        if (request == null || request.role() == null || request.role().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "role is required");
        }
        try {
            return ClubRole.valueOf(request.role().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Invalid role: " + request.role());
        }
    }
}
