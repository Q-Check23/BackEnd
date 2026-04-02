package team23.q_check.club.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team23.q_check.club.domain.model.Club;
import team23.q_check.club.domain.model.ClubMember;
import team23.q_check.club.domain.model.ClubRole;
import team23.q_check.club.dto.AddClubMemberRequestDto;
import team23.q_check.club.dto.ClubMemberResponseDto;
import team23.q_check.club.dto.ClubResponseDto;
import team23.q_check.club.dto.CreateClubRequestDto;
import team23.q_check.club.dto.MyClubResponseDto;
import team23.q_check.club.dto.UpdateClubMemberRoleRequestDto;
import team23.q_check.club.domain.repository.ClubMemberRepository;
import team23.q_check.club.domain.repository.ClubRepository;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.identity.domain.model.User;
import team23.q_check.identity.domain.repository.UserRepository;

import java.util.List;

@Service
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;
    private final ClubAuthorizationService clubAuthorizationService;

    public ClubService(
            ClubRepository clubRepository,
            ClubMemberRepository clubMemberRepository,
            UserRepository userRepository,
            ClubAuthorizationService clubAuthorizationService
    ) {
        this.clubRepository = clubRepository;
        this.clubMemberRepository = clubMemberRepository;
        this.userRepository = userRepository;
        this.clubAuthorizationService = clubAuthorizationService;
    }

    public ClubResponseDto getSampleClub() {
        return new ClubResponseDto(1L, "UMC", "University MakeUs Challenge club");
    }

    @Transactional
    public ClubResponseDto createClub(Long currentUserId, CreateClubRequestDto request) {
        validateCreateRequest(request);

        if (clubRepository.existsByDiscordGuildId(request.discordGuildId())) {
            throw new AppException(ErrorCode.CONFLICT, "Club already exists with discordGuildId: " + request.discordGuildId());
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found: " + currentUserId));

        Club club = new Club(
                request.name().trim(),
                request.description(),
                request.discordGuildId().trim(),
                request.coverImageUrl()
        );
        Club savedClub = clubRepository.save(club);
        ClubMember ownerMembership = new ClubMember(savedClub, currentUser, ClubRole.OWNER);
        clubMemberRepository.save(ownerMembership);

        return new ClubResponseDto(savedClub.getId(), savedClub.getName(), savedClub.getDescription());
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
        if (request.discordGuildId() == null || request.discordGuildId().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "discordGuildId is required");
        }
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
