package team23.q_check.event.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team23.q_check.event.dto.EventResponseDto;
import team23.q_check.event.service.EventService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventControllerTest {

    private MockMvc mockMvc;
    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = mock(EventService.class);
        EventController eventController = new EventController(eventService);
        mockMvc = MockMvcBuilders.standaloneSetup(eventController).build();
    }

    @Test
    void getSampleEvent_returnsApiResponse() throws Exception {
        when(eventService.getSampleEvent()).thenReturn(new EventResponseDto(10L, "Spring Boot Study", "2026-03-05T19:00:00"));

        mockMvc.perform(get("/api/events/sample"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.title").value("Spring Boot Study"));
    }
}
