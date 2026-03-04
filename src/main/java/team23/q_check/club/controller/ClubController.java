package team23.q_check.club.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team23.q_check.club.dto.ClubResponseDto;
import team23.q_check.club.service.ClubService;
import team23.q_check.common.response.ApiResponse;

@Tag(name = "Club", description = "Club API")
@RestController
@RequestMapping("/api/clubs")
public class ClubController {

    private final ClubService clubService;

    public ClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    @Operation(summary = "샘플 동아리 조회")
    @GetMapping("/sample")
    public ApiResponse<ClubResponseDto> getSampleClub() {
        return ApiResponse.ok(clubService.getSampleClub());
    }
}
