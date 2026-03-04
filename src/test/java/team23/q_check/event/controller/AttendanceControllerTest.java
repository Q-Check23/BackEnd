package team23.q_check.event.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team23.q_check.common.auth.CurrentUserIdArgumentResolver;
import team23.q_check.common.error.GlobalExceptionHandler;
import team23.q_check.event.dto.CheckInResponseDto;
import team23.q_check.event.service.AttendanceService;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AttendanceControllerTest {

    private MockMvc mockMvc;
    private AttendanceService attendanceService;

    @BeforeEach
    void setUp() {
        attendanceService = mock(AttendanceService.class);
        AttendanceController controller = new AttendanceController(attendanceService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void checkIn_returnsResponse() throws Exception {
        when(attendanceService.checkIn(anyLong(), anyString()))
                .thenReturn(new CheckInResponseDto(200L, "2026-03-10T19:20:00", "김지윤"));

        mockMvc.perform(post("/api/attendance/check-in")
                        .header("X-USER-ID", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrToken\":\"550e8400-e29b-41d4-a716-446655440000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registrationId").value(200))
                .andExpect(jsonPath("$.data.username").value("김지윤"));
    }

    @Test
    void checkIn_withoutHeader_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/attendance/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrToken\":\"550e8400-e29b-41d4-a716-446655440000\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
