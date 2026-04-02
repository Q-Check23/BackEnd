package team23.q_check.identity.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team23.q_check.identity.dto.UserResponseDto;
import team23.q_check.identity.domain.service.UserService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IdentityControllerTest {

    private MockMvc mockMvc;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        IdentityController identityController = new IdentityController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(identityController).build();
    }

    @Test
    void getSampleUser_returnsApiResponse() throws Exception {
        when(userService.getSampleUser()).thenReturn(new UserResponseDto(7L, "123456789012345678", "kimjyun"));

        mockMvc.perform(get("/api/identities/sample"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.username").value("kimjyun"));
    }
}
