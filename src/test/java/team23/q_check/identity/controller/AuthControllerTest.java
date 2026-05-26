package team23.q_check.identity.controller;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team23.q_check.common.config.FrontendProperties;
import team23.q_check.common.config.JwtProperties;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
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

    private static final String FRONT_CALLBACK = "https://qcheck.asia/auth/callback";

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        JwtProperties jwtProperties = new JwtProperties("test-secret-key-of-at-least-32-bytes!", 3_600_000L,
                604_800_000L, 600_000L);
        FrontendProperties frontendProperties = new FrontendProperties(FRONT_CALLBACK);
        AuthController controller = new AuthController(authService, jwtProperties, frontendProperties);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_redirectsToDiscordAndStoresStateInSession() throws Exception {
        when(authService.getAuthorizationUrl(any())).thenReturn("https://discord.com/oauth2/authorize?...");

        var result = mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://discord.com/oauth2/authorize?..."))
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);
        assert session != null;
        assert session.getAttribute(OAUTH_STATE_SESSION_KEY) != null;
    }

    @Test
    void handleCode_existingUser_redirectsToFrontWithAccessTokenAndSetsRefreshCookie() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(OAUTH_STATE_SESSION_KEY, "state-1");
        when(authService.processCode("auth-code"))
                .thenReturn(new CodeResult(false, "access-token", "refresh-token"));

        mockMvc.perform(get("/api/auth/code")
                        .session(session)
                        .param("code", "auth-code")
                        .param("state", "state-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(FRONT_CALLBACK + "?token=access-token&isNewUser=false"))
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(cookie().value("refresh_token", "refresh-token"))
                .andExpect(cookie().httpOnly("refresh_token", true));
    }

    @Test
    void handleCode_newUser_redirectsWithSignupTokenAndNoCookie() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(OAUTH_STATE_SESSION_KEY, "state-1");
        when(authService.processCode("auth-code"))
                .thenReturn(new CodeResult(true, "signup-token", null));

        mockMvc.perform(get("/api/auth/code")
                        .session(session)
                        .param("code", "auth-code")
                        .param("state", "state-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(FRONT_CALLBACK + "?token=signup-token&isNewUser=true"))
                .andExpect(cookie().doesNotExist("refresh_token"));
    }

    @Test
    void handleCode_stateMismatch_redirectsWithError() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(OAUTH_STATE_SESSION_KEY, "expected-state");

        mockMvc.perform(get("/api/auth/code")
                        .session(session)
                        .param("code", "auth-code")
                        .param("state", "different-state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        FRONT_CALLBACK + "?error=INVALID_REQUEST&message=OAuth%20state%EA%B0%80%20%EC%9C%A0%ED%9A%A8%ED%95%98%EC%A7%80%20%EC%95%8A%EC%8A%B5%EB%8B%88%EB%8B%A4"));
    }

    @Test
    void handleCode_serviceFailure_redirectsWithError() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(OAUTH_STATE_SESSION_KEY, "state-1");
        when(authService.processCode("auth-code"))
                .thenThrow(new AppException(ErrorCode.UNAUTHORIZED, "Discord 인가코드 교환에 실패했습니다"));

        mockMvc.perform(get("/api/auth/code")
                        .session(session)
                        .param("code", "auth-code")
                        .param("state", "state-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("error=UNAUTHORIZED")));
    }

    @Test
    void checkUsername_returnsAvailability() throws Exception {
        when(authService.isUsernameAvailable("free-nick")).thenReturn(true);

        mockMvc.perform(get("/api/auth/username/check").param("username", "free-nick"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void signup_validBearer_returnsAccessTokenAndSetsRefreshCookie() throws Exception {
        when(authService.signup(any(), any()))
                .thenReturn(new TokenResult(7L, "access-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/signup")
                        .header("Authorization", "Bearer signup-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "김지윤", "username": "kimjyun", "email": "kim@example.com" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(7))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(cookie().value("refresh_token", "refresh-token"));
    }

    @Test
    void signup_missingAuthorizationHeader_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "김지윤", "username": "kimjyun", "email": "kim@example.com" }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void signup_malformedAuthorizationHeader_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .header("Authorization", "signup-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "김지윤", "username": "kimjyun", "email": "kim@example.com" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_rotatesTokensFromCookie() throws Exception {
        when(authService.refresh("old-refresh"))
                .thenReturn(new TokenResult(7L, "new-access", "new-refresh"));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "old-refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(7))
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(cookie().value("refresh_token", "new-refresh"));
    }
}
