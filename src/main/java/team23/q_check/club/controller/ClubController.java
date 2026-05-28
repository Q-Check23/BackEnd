package team23.q_check.club.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team23.q_check.club.dto.AddClubMemberRequestDto;
import team23.q_check.club.dto.ClubDetailResponseDto;
import team23.q_check.club.dto.ClubMemberResponseDto;
import team23.q_check.club.dto.CreateClubRequestDto;
import team23.q_check.club.dto.JoinClubRequestDto;
import team23.q_check.club.dto.MyClubResponseDto;
import team23.q_check.club.dto.UpdateClubMemberRoleRequestDto;
import team23.q_check.club.dto.UpdateClubRequestDto;
import team23.q_check.club.domain.service.ClubService;
import team23.q_check.common.auth.CurrentUserId;
import team23.q_check.common.response.ApiResponse;

import java.util.List;

@Tag(name = "Club", description = "Club API")
@SecurityRequirement(name = "X-USER-ID")
@RestController
@RequestMapping("/api/clubs")
public class ClubController {

    private final ClubService clubService;

    public ClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    @Operation(summary = "클럽 생성 (생성자는 OWNER로 자동 가입)")
    @PostMapping
    public ApiResponse<MyClubResponseDto> createClub(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @RequestBody CreateClubRequestDto request
    ) {
        return ApiResponse.ok(clubService.createClub(currentUserId, request));
    }

    @Operation(summary = "초대 코드로 클럽 가입")
    @PostMapping("/join")
    public ApiResponse<MyClubResponseDto> joinClub(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @RequestBody JoinClubRequestDto request
    ) {
        return ApiResponse.ok(clubService.joinClub(currentUserId, request));
    }

    @Operation(summary = "행사를 통해 해당 클럽에 가입 (이미 멤버면 멱등)")
    @PostMapping("/join-via-event/{eventId}")
    public ApiResponse<MyClubResponseDto> joinClubViaEvent(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long eventId
    ) {
        return ApiResponse.ok(clubService.joinClubViaEvent(currentUserId, eventId));
    }

    @Operation(summary = "내가 속한 클럽 목록 조회")
    @GetMapping
    public ApiResponse<List<MyClubResponseDto>> getMyClubs(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId
    ) {
        return ApiResponse.ok(clubService.getMyClubs(currentUserId));
    }

    @Operation(summary = "클럽 상세 조회 (멤버 전용)")
    @GetMapping("/{clubId}")
    public ApiResponse<ClubDetailResponseDto> getClubDetail(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long clubId
    ) {
        return ApiResponse.ok(clubService.getClubDetail(currentUserId, clubId));
    }

    @Operation(summary = "클럽 정보 수정 (OWNER/ADMIN)")
    @PutMapping("/{clubId}")
    public ApiResponse<ClubDetailResponseDto> updateClub(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long clubId,
            @RequestBody UpdateClubRequestDto request
    ) {
        return ApiResponse.ok(clubService.updateClub(currentUserId, clubId, request));
    }

    @Operation(summary = "클럽 삭제 (OWNER만, 빈 클럽일 때만)",
            description = "이벤트·공지가 모두 없고 본인 외 멤버가 없을 때만 삭제됩니다. 그 외엔 409.")
    @DeleteMapping("/{clubId}")
    public ApiResponse<Void> deleteClub(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long clubId
    ) {
        clubService.deleteClub(currentUserId, clubId);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "초대 코드 조회 (OWNER/ADMIN)")
    @GetMapping("/{clubId}/invite-code")
    public ApiResponse<String> getInviteCode(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long clubId
    ) {
        return ApiResponse.ok(clubService.getInviteCode(currentUserId, clubId));
    }

    @Operation(summary = "클럽 멤버 목록 조회")
    @GetMapping("/{clubId}/members")
    public ApiResponse<List<ClubMemberResponseDto>> getClubMembers(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long clubId
    ) {
        return ApiResponse.ok(clubService.getClubMembers(currentUserId, clubId));
    }

    @Operation(summary = "클럽 멤버 추가 (OWNER/ADMIN)")
    @PostMapping("/{clubId}/members")
    public ApiResponse<ClubMemberResponseDto> addClubMember(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long clubId,
            @RequestBody AddClubMemberRequestDto request
    ) {
        return ApiResponse.ok(clubService.addClubMember(currentUserId, clubId, request));
    }

    @Operation(summary = "클럽 멤버 역할 변경 (OWNER/ADMIN)")
    @PutMapping("/{clubId}/members/{memberId}/role")
    public ApiResponse<ClubMemberResponseDto> updateClubMemberRole(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long clubId,
            @PathVariable Long memberId,
            @RequestBody UpdateClubMemberRoleRequestDto request
    ) {
        return ApiResponse.ok(clubService.updateClubMemberRole(currentUserId, clubId, memberId, request));
    }

    @Operation(summary = "클럽 탈퇴 (본인)",
            description = "본인이 마지막 OWNER이면 409 (먼저 다른 사람에게 OWNER 위임 필요).")
    @DeleteMapping("/{clubId}/members/me")
    public ApiResponse<Void> leaveClub(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long clubId
    ) {
        clubService.leaveClub(currentUserId, clubId);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "클럽 멤버 제거 (OWNER/ADMIN)",
            description = "ADMIN은 OWNER를 제거할 수 없으며, 본인을 제거하려면 /me 엔드포인트를 사용해야 합니다.")
    @DeleteMapping("/{clubId}/members/{memberId}")
    public ApiResponse<Void> removeClubMember(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @PathVariable Long clubId,
            @PathVariable Long memberId
    ) {
        clubService.removeClubMember(currentUserId, clubId, memberId);
        return ApiResponse.ok(null);
    }

}
