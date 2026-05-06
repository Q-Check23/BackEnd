package team23.q_check.club.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import team23.q_check.club.domain.model.Club;
import team23.q_check.club.domain.model.ClubMember;
import team23.q_check.club.domain.model.ClubRole;
import team23.q_check.club.domain.service.ClubAuthorizationService;
import team23.q_check.club.domain.service.ClubService;
import team23.q_check.club.dto.AddClubMemberRequestDto;
import team23.q_check.club.dto.ClubResponseDto;
import team23.q_check.club.dto.CreateClubRequestDto;
import team23.q_check.club.dto.UpdateClubMemberRoleRequestDto;
import team23.q_check.club.domain.repository.ClubMemberRepository;
import team23.q_check.club.domain.repository.ClubRepository;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.identity.domain.model.User;
import team23.q_check.identity.domain.repository.UserRepository;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClubServiceTest {

    private ClubRepository clubRepository;
    private ClubMemberRepository clubMemberRepository;
    private UserRepository userRepository;
    private ClubAuthorizationService clubAuthorizationService;
    private ClubService clubService;

    @BeforeEach
    void setUp() {
        clubRepository = mock(ClubRepository.class);
        clubMemberRepository = mock(ClubMemberRepository.class);
        userRepository = mock(UserRepository.class);
        clubAuthorizationService = mock(ClubAuthorizationService.class);
        clubService = new ClubService(clubRepository, clubMemberRepository, userRepository, clubAuthorizationService);
    }

    @Test
    void createClub_savesClubAndOwnerMembership() throws Exception {
        User currentUser = new User("dev-1", null, "owner", null);
        setId(currentUser, 1L);

        when(clubRepository.existsByDiscordGuildId("guild-1")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        when(clubRepository.save(any(Club.class))).thenAnswer(invocation -> {
            Club club = invocation.getArgument(0);
            setId(club, 100L);
            return club;
        });

        ClubResponseDto result = clubService.createClub(
                1L,
                new CreateClubRequestDto("UMC", "desc", "guild-1", null)
        );

        assertEquals(100L, result.id());
        assertEquals("UMC", result.name());

        ArgumentCaptor<ClubMember> membershipCaptor = ArgumentCaptor.forClass(ClubMember.class);
        verify(clubMemberRepository).save(membershipCaptor.capture());
        assertEquals(ClubRole.OWNER, membershipCaptor.getValue().getRole());
        assertEquals(1L, membershipCaptor.getValue().getUser().getId());
    }

    @Test
    void addClubMember_whenOperatorIsMember_throwsForbidden() throws Exception {
        when(clubAuthorizationService.requireAdminOrOwner(1L, 1L))
                .thenThrow(new AppException(ErrorCode.FORBIDDEN, "Only OWNER or ADMIN can perform this action"));

        AppException exception = assertThrows(
                AppException.class,
                () -> clubService.addClubMember(1L, 1L, new AddClubMemberRequestDto(7L))
        );
        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void leaveClub_lastOwner_throwsConflict() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null);
        setId(club, 1L);
        User owner = new User("dev-1", null, "owner", null);
        setId(owner, 1L);
        ClubMember ownerMembership = new ClubMember(club, owner, ClubRole.OWNER);
        setId(ownerMembership, 10L);

        when(clubAuthorizationService.requireMembership(1L, 1L)).thenReturn(ownerMembership);
        when(clubMemberRepository.countByClub_IdAndRole(1L, ClubRole.OWNER)).thenReturn(1L);

        AppException exception = assertThrows(
                AppException.class,
                () -> clubService.leaveClub(1L, 1L)
        );
        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(clubMemberRepository, never()).delete(any());
    }

    @Test
    void leaveClub_memberLeavesSuccessfully() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null);
        setId(club, 1L);
        User member = new User("dev-2", null, "member", null);
        setId(member, 2L);
        ClubMember membership = new ClubMember(club, member, ClubRole.MEMBER);
        setId(membership, 11L);

        when(clubAuthorizationService.requireMembership(1L, 2L)).thenReturn(membership);

        clubService.leaveClub(2L, 1L);

        verify(clubMemberRepository).delete(membership);
    }

    @Test
    void leaveClub_ownerWhenAnotherOwnerExists_succeeds() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null);
        setId(club, 1L);
        User owner = new User("dev-1", null, "owner", null);
        setId(owner, 1L);
        ClubMember ownerMembership = new ClubMember(club, owner, ClubRole.OWNER);
        setId(ownerMembership, 10L);

        when(clubAuthorizationService.requireMembership(1L, 1L)).thenReturn(ownerMembership);
        when(clubMemberRepository.countByClub_IdAndRole(1L, ClubRole.OWNER)).thenReturn(2L);

        clubService.leaveClub(1L, 1L);

        verify(clubMemberRepository).delete(ownerMembership);
    }

    @Test
    void removeClubMember_adminCannotRemoveOwner() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null);
        setId(club, 1L);
        User admin = new User("dev-1", null, "admin", null);
        setId(admin, 1L);
        User owner = new User("dev-2", null, "owner", null);
        setId(owner, 2L);
        ClubMember adminMembership = new ClubMember(club, admin, ClubRole.ADMIN);
        setId(adminMembership, 10L);
        ClubMember ownerMembership = new ClubMember(club, owner, ClubRole.OWNER);
        setId(ownerMembership, 11L);

        when(clubAuthorizationService.requireAdminOrOwner(1L, 1L)).thenReturn(adminMembership);
        when(clubMemberRepository.findByIdAndClub_Id(11L, 1L)).thenReturn(Optional.of(ownerMembership));

        AppException exception = assertThrows(
                AppException.class,
                () -> clubService.removeClubMember(1L, 1L, 11L)
        );
        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        verify(clubMemberRepository, never()).delete(any());
    }

    @Test
    void removeClubMember_cannotRemoveSelf() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null);
        setId(club, 1L);
        User admin = new User("dev-1", null, "admin", null);
        setId(admin, 1L);
        ClubMember adminMembership = new ClubMember(club, admin, ClubRole.ADMIN);
        setId(adminMembership, 10L);

        when(clubAuthorizationService.requireAdminOrOwner(1L, 1L)).thenReturn(adminMembership);
        when(clubMemberRepository.findByIdAndClub_Id(10L, 1L)).thenReturn(Optional.of(adminMembership));

        AppException exception = assertThrows(
                AppException.class,
                () -> clubService.removeClubMember(1L, 1L, 10L)
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void removeClubMember_lastOwnerByOwner_throwsConflict() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null);
        setId(club, 1L);
        User owner1 = new User("dev-1", null, "owner1", null);
        setId(owner1, 1L);
        User owner2 = new User("dev-2", null, "owner2", null);
        setId(owner2, 2L);
        ClubMember owner1Membership = new ClubMember(club, owner1, ClubRole.OWNER);
        setId(owner1Membership, 10L);
        ClubMember owner2Membership = new ClubMember(club, owner2, ClubRole.OWNER);
        setId(owner2Membership, 11L);

        when(clubAuthorizationService.requireAdminOrOwner(1L, 1L)).thenReturn(owner1Membership);
        when(clubMemberRepository.findByIdAndClub_Id(11L, 1L)).thenReturn(Optional.of(owner2Membership));
        when(clubMemberRepository.countByClub_IdAndRole(1L, ClubRole.OWNER)).thenReturn(1L);

        AppException exception = assertThrows(
                AppException.class,
                () -> clubService.removeClubMember(1L, 1L, 11L)
        );
        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    @Test
    void removeClubMember_adminRemovesMemberSuccessfully() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null);
        setId(club, 1L);
        User admin = new User("dev-1", null, "admin", null);
        setId(admin, 1L);
        User member = new User("dev-2", null, "member", null);
        setId(member, 2L);
        ClubMember adminMembership = new ClubMember(club, admin, ClubRole.ADMIN);
        setId(adminMembership, 10L);
        ClubMember memberMembership = new ClubMember(club, member, ClubRole.MEMBER);
        setId(memberMembership, 11L);

        when(clubAuthorizationService.requireAdminOrOwner(1L, 1L)).thenReturn(adminMembership);
        when(clubMemberRepository.findByIdAndClub_Id(11L, 1L)).thenReturn(Optional.of(memberMembership));

        clubService.removeClubMember(1L, 1L, 11L);

        verify(clubMemberRepository).delete(memberMembership);
    }

    @Test
    void updateClubMemberRole_adminCannotAssignOwner() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null);
        setId(club, 1L);
        User admin = new User("dev-1", null, "admin", null);
        setId(admin, 1L);
        User target = new User("dev-2", null, "target", null);
        setId(target, 7L);

        ClubMember adminMembership = new ClubMember(club, admin, ClubRole.ADMIN);
        ClubMember targetMembership = new ClubMember(club, target, ClubRole.MEMBER);
        setId(targetMembership, 10L);

        when(clubAuthorizationService.requireAdminOrOwner(1L, 1L)).thenReturn(adminMembership);
        when(clubMemberRepository.findByIdAndClub_Id(10L, 1L)).thenReturn(Optional.of(targetMembership));

        AppException exception = assertThrows(
                AppException.class,
                () -> clubService.updateClubMemberRole(1L, 1L, 10L, new UpdateClubMemberRoleRequestDto("OWNER"))
        );
        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    private void setId(Object target, Long id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
