package team23.q_check.club.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team23.q_check.club.dto.AddClubMemberRequestDto;
import team23.q_check.club.dto.ClubMemberResponseDto;
import team23.q_check.club.dto.ClubResponseDto;
import team23.q_check.club.dto.CreateClubRequestDto;
import team23.q_check.club.dto.MyClubResponseDto;
import team23.q_check.club.dto.UpdateClubMemberRoleRequestDto;
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
    public ApiResponse<ClubResponseDto> createClub(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId,
            @RequestBody CreateClubRequestDto request
    ) {
        return ApiResponse.ok(clubService.createClub(currentUserId, request));
    }

    @Operation(summary = "내가 속한 클럽 목록 조회")
    @GetMapping
    public ApiResponse<List<MyClubResponseDto>> getMyClubs(
            @Parameter(hidden = true)
            @CurrentUserId Long currentUserId
    ) {
        return ApiResponse.ok(clubService.getMyClubs(currentUserId));
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

    @Operation(summary = "샘플 동아리 조회")
    @GetMapping("/sample")
    public ApiResponse<ClubResponseDto> getSampleClub() {
        return ApiResponse.ok(clubService.getSampleClub());
    }
}
