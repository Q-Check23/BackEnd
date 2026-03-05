package team23.q_check.club.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team23.q_check.club.domain.model.ClubRole;
import team23.q_check.club.dto.ClubMemberResponseDto;
import team23.q_check.club.dto.ClubResponseDto;
import team23.q_check.club.dto.MyClubResponseDto;
import team23.q_check.club.service.ClubService;
import team23.q_check.common.auth.CurrentUserIdArgumentResolver;
import team23.q_check.common.error.GlobalExceptionHandler;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClubControllerTest {

    private MockMvc mockMvc;
    private ClubService clubService;

    @BeforeEach
    void setUp() {
        clubService = mock(ClubService.class);
        ClubController clubController = new ClubController(clubService);
        mockMvc = MockMvcBuilders.standaloneSetup(clubController)
                .setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createClub_returnsCreatedClub() throws Exception {
        when(clubService.createClub(anyLong(), any())).thenReturn(new ClubResponseDto(1L, "UMC", "Club desc"));

        mockMvc.perform(post("/api/clubs")
                        .header("X-USER-ID", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"UMC\",\"description\":\"Club desc\",\"discordGuildId\":\"guild-1\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("UMC"));
    }

    @Test
    void getMyClubs_returnsList() throws Exception {
        when(clubService.getMyClubs(1L))
                .thenReturn(List.of(new MyClubResponseDto(1L, "UMC", "Club desc", ClubRole.OWNER)));

        mockMvc.perform(get("/api/clubs").header("X-USER-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].clubId").value(1))
                .andExpect(jsonPath("$.data[0].myRole").value("OWNER"));
    }

    @Test
    void getClubMembers_returnsMembers() throws Exception {
        when(clubService.getClubMembers(1L, 1L))
                .thenReturn(List.of(new ClubMemberResponseDto(10L, 7L, "kimjyun", ClubRole.MEMBER)));

        mockMvc.perform(get("/api/clubs/1/members").header("X-USER-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].memberId").value(10))
                .andExpect(jsonPath("$.data[0].role").value("MEMBER"));
    }

    @Test
    void addClubMember_returnsMember() throws Exception {
        when(clubService.addClubMember(anyLong(), anyLong(), any()))
                .thenReturn(new ClubMemberResponseDto(10L, 7L, "kimjyun", ClubRole.MEMBER));

        mockMvc.perform(post("/api/clubs/1/members")
                        .header("X-USER-ID", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(7));
    }

    @Test
    void updateClubMemberRole_returnsUpdatedRole() throws Exception {
        when(clubService.updateClubMemberRole(anyLong(), anyLong(), anyLong(), any()))
                .thenReturn(new ClubMemberResponseDto(10L, 7L, "kimjyun", ClubRole.ADMIN));

        mockMvc.perform(put("/api/clubs/1/members/10/role")
                        .header("X-USER-ID", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void getMyClubs_withoutHeader_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/clubs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
