package team23.q_check.identity.controller;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team23.q_check.common.config.JwtProperties;
import team23.q_check.common.error.GlobalExceptionHandler;
import team23.q_check.identity.domain.service.AuthService;
import team23.q_check.identity.domain.service.AuthService.CodeResult;
import team23.q_check.identity.domain.service.AuthService.TokenResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private static final String OAUTH_STATE_SESSION_KEY = "oauth2_state";

    private MockMvc mockMvc;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        JwtProperties jwtProperties = new JwtProperties("test-secret-key-of-at-least-32-bytes!", 3_600_000L,
                604_800_000L, 600_000L);
        AuthController controller = new AuthController(authService, jwtProperties);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_redirectsToDiscordAndStoresStateInSession() throws Exception {
        when(authService.getAuthorizationUrl(any())).thenReturn("https://discord.com/oauth2/authorize?...");

        var result = mockMvc.perform(get("/auth/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://discord.com/oauth2/authorize?..."))
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);
        assert session != null;
        assert session.getAttribute(OAUTH_STATE_SESSION_KEY) != null;
    }

    @Test
    void handleCode_existingUser_setsRefreshCookieAndReturnsAccessToken() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(OAUTH_STATE_SESSION_KEY, "state-1");
        when(authService.processCode("auth-code"))
                .thenReturn(new CodeResult(false, "access-token", "refresh-token"));

        mockMvc.perform(get("/auth/code")
                        .session(session)
                        .param("code", "auth-code")
                        .param("state", "state-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isNewUser").value(false))
                .andExpect(jsonPath("$.data.token").value("access-token"))
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(cookie().value("refresh_token", "refresh-token"))
                .andExpect(cookie().httpOnly("refresh_token", true));
    }

    @Test
    void handleCode_newUser_doesNotSetRefreshCookie() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(OAUTH_STATE_SESSION_KEY, "state-1");
        when(authService.processCode("auth-code"))
                .thenReturn(new CodeResult(true, "signup-token", null));

        mockMvc.perform(get("/auth/code")
                        .session(session)
                        .param("code", "auth-code")
                        .param("state", "state-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isNewUser").value(true))
                .andExpect(jsonPath("$.data.token").value("signup-token"))
                .andExpect(cookie().doesNotExist("refresh_token"));
    }

    @Test
    void handleCode_stateMismatch_returnsBadRequest() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(OAUTH_STATE_SESSION_KEY, "expected-state");

        mockMvc.perform(get("/auth/code")
                        .session(session)
                        .param("code", "auth-code")
                        .param("state", "different-state"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void handleCode_noSessionState_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/auth/code")
                        .param("code", "auth-code")
                        .param("state", "state-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void checkNickname_returnsAvailability() throws Exception {
        when(authService.isNicknameAvailable("free-nick")).thenReturn(true);

        mockMvc.perform(get("/auth/nickname/check").param("nickname", "free-nick"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void signup_validBearer_returnsAccessTokenAndSetsRefreshCookie() throws Exception {
        when(authService.signup(any(), any()))
                .thenReturn(new TokenResult(7L, "access-token", "refresh-token"));

        mockMvc.perform(post("/auth/signup")
                        .header("Authorization", "Bearer signup-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "김지윤", "nickname": "kimjyun", "email": "kim@example.com" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(7))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(cookie().value("refresh_token", "refresh-token"));
    }

    @Test
    void signup_malformedAuthorizationHeader_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .header("Authorization", "signup-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "김지윤", "nickname": "kimjyun", "email": "kim@example.com" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_rotatesTokensFromCookie() throws Exception {
        when(authService.refresh("old-refresh"))
                .thenReturn(new TokenResult(7L, "new-access", "new-refresh"));

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "old-refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(7))
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(cookie().value("refresh_token", "new-refresh"));
    }
}
