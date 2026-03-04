package team23.q_check.club.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team23.q_check.club.dto.ClubResponseDto;
import team23.q_check.club.service.ClubService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        mockMvc = MockMvcBuilders.standaloneSetup(clubController).build();
    }

    @Test
    void getSampleClub_returnsApiResponse() throws Exception {
        when(clubService.getSampleClub()).thenReturn(new ClubResponseDto(1L, "UMC", "University MakeUs Challenge club"));

        mockMvc.perform(get("/api/clubs/sample"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("UMC"));
    }
}
