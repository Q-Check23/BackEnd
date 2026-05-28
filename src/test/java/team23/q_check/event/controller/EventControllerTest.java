package team23.q_check.event.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team23.q_check.common.auth.CurrentUserIdArgumentResolver;
import team23.q_check.common.auth.JwtService;
import team23.q_check.common.error.GlobalExceptionHandler;
import team23.q_check.event.dto.EventDetailResponseDto;
import team23.q_check.event.dto.EventListItemDto;
import team23.q_check.event.dto.EventPageResponseDto;
import team23.q_check.event.dto.FormFieldResponseDto;
import team23.q_check.event.domain.service.EventService;
import team23.q_check.event.domain.service.RegistrationService;
import team23.q_check.event.dto.CreateRegistrationResponseDto;
import team23.q_check.event.dto.MyRegistrationResponseDto;
import team23.q_check.event.dto.AdminRegistrationItemDto;
import team23.q_check.event.dto.RegistrationAnswerResponseDto;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventControllerTest {

    private MockMvc mockMvc;
    private EventService eventService;
    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        eventService = mock(EventService.class);
        registrationService = mock(RegistrationService.class);
        EventController eventController = new EventController(eventService, registrationService);
        mockMvc = MockMvcBuilders.standaloneSetup(eventController)
                .setCustomArgumentResolvers(new CurrentUserIdArgumentResolver(mock(JwtService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createEvent_returnsCreatedEvent() throws Exception {
        when(eventService.createEvent(anyLong(), any())).thenReturn(
                new EventDetailResponseDto(
                        100L, 1L, "OT", null,
                        "2026-03-10T19:00", null, null, null,
                        java.math.BigDecimal.ZERO, true,
                        List.of(new FormFieldResponseDto(1L, "TEXT", "학번", true, List.of()))
                )
        );

        mockMvc.perform(post("/api/events")
                        .header("X-USER-ID", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clubId": 1,
                                  "title": "OT",
                                  "startTime": "2026-03-10T19:00:00",
                                  "formFields": [{"type":"TEXT","label":"학번","required":true}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventId").value(100));
    }

    @Test
    void getEvents_returnsPagedItems() throws Exception {
        when(eventService.getEvents(anyInt(), anyInt(), org.mockito.ArgumentMatchers.isNull())).thenReturn(
                new EventPageResponseDto(
                        0, 10, 1, 1,
                        List.of(new EventListItemDto(
                                100L, "OT", "2026-03-10T19:00", "2026-03-10T22:00",
                                "강남", java.math.BigDecimal.ZERO, true))
                )
        );

        mockMvc.perform(get("/api/events?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].eventId").value(100))
                .andExpect(jsonPath("$.data.items[0].title").value("OT"));
    }

    @Test
    void getEvent_returnsDetail() throws Exception {
        when(eventService.getEvent(100L)).thenReturn(
                new EventDetailResponseDto(
                        100L, 1L, "OT", null,
                        "2026-03-10T19:00", "2026-03-10T22:00", "강남", null,
                        java.math.BigDecimal.ZERO, true, List.of())
        );

        mockMvc.perform(get("/api/events/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventId").value(100));
    }

    @Test
    void updateEvent_withoutHeader_returnsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/events/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\":\"강남\",\"isActive\":false}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void createRegistration_returnsRegistrationInfo() throws Exception {
        when(registrationService.createRegistration(anyLong(), anyLong(), any()))
                .thenReturn(new CreateRegistrationResponseDto(200L, "550e8400-e29b-41d4-a716-446655440000"));

        mockMvc.perform(post("/api/events/100/registrations")
                        .header("X-USER-ID", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers":[
                                    {"fieldId": 11, "value":"한식"},
                                    {"fieldId": 12, "value":"컴공 23학번"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registrationId").value(200))
                .andExpect(jsonPath("$.data.qrToken").exists());
    }

    @Test
    void getMyRegistration_returnsMyData() throws Exception {
        when(registrationService.getMyRegistration(1L, 100L))
                .thenReturn(new MyRegistrationResponseDto(
                        200L,
                        "550e8400-e29b-41d4-a716-446655440000",
                        "REGISTERED",
                        "2026-03-10T19:10:00",
                        List.of(new RegistrationAnswerResponseDto(11L, "식사 메뉴", "한식"))
                ));

        mockMvc.perform(get("/api/events/100/registrations/me").header("X-USER-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REGISTERED"))
                .andExpect(jsonPath("$.data.answers[0].value").value("한식"));
    }

    @Test
    void getEventRegistrations_returnsAdminList() throws Exception {
        when(registrationService.getEventRegistrations(1L, 100L))
                .thenReturn(List.of(new AdminRegistrationItemDto(
                        200L, 7L, "kimjyun", "김지윤", "REGISTERED",
                        "550e8400-e29b-41d4-a716-446655440000",
                        List.of(new RegistrationAnswerResponseDto(11L, "식사 메뉴", "한식"))
                )));

        mockMvc.perform(get("/api/events/100/registrations").header("X-USER-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].registrationId").value(200))
                .andExpect(jsonPath("$.data[0].answers[0].label").value("식사 메뉴"));
    }
}
