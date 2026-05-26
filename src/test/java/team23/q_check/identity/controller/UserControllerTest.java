package team23.q_check.identity.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team23.q_check.common.auth.CurrentUserIdArgumentResolver;
import team23.q_check.common.auth.JwtService;
import team23.q_check.common.error.GlobalExceptionHandler;
import team23.q_check.identity.dto.MyUserResponseDto;
import team23.q_check.identity.dto.UserSearchResultDto;
import team23.q_check.identity.domain.service.UserService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    private MockMvc mockMvc;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        UserController userController = new UserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(new CurrentUserIdArgumentResolver(mock(JwtService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getMyUser_returnsCurrentUser() throws Exception {
        when(userService.getMyUser(1L)).thenReturn(new MyUserResponseDto(1L, "qcheck_user", "김지윤", "user@example.com", "010-1234-5678"));

        mockMvc.perform(get("/api/users/me").header("X-USER-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("qcheck_user"));
    }

    @Test
    void putMyUser_updatesRealName() throws Exception {
        when(userService.updateMyUser(any(Long.class), any()))
                .thenReturn(new MyUserResponseDto(1L, "qcheck_user", "홍길동", "user@example.com", "010-1234-5678"));

        mockMvc.perform(put("/api/users/me")
                        .header("X-USER-ID", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"realName\":\"홍길동\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.realName").value("홍길동"));
    }

    @Test
    void getMyUser_withoutHeader_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void getMyUser_withInvalidHeader_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/users/me").header("X-USER-ID", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void searchUsers_byUsername_returnsList() throws Exception {
        when(userService.searchUsers("kim", null)).thenReturn(List.of(
                new UserSearchResultDto(1L, "kimjyun", "김지윤"),
                new UserSearchResultDto(2L, "kimminseo", null)
        ));

        mockMvc.perform(get("/api/users/search?username=kim").header("X-USER-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].userId").value(1))
                .andExpect(jsonPath("$.data[0].username").value("kimjyun"))
                .andExpect(jsonPath("$.data[0].realName").value("김지윤"))
                .andExpect(jsonPath("$.data[0].email").doesNotExist());
    }

    @Test
    void searchUsers_byEmail_returnsSingle() throws Exception {
        when(userService.searchUsers(null, "kim@example.com")).thenReturn(List.of(
                new UserSearchResultDto(1L, "kimjyun", "김지윤")
        ));

        mockMvc.perform(get("/api/users/search?email=kim@example.com").header("X-USER-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].userId").value(1));
    }
}
