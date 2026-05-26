package team23.q_check.identity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import team23.q_check.common.auth.JwtService;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.identity.domain.model.User;
import team23.q_check.identity.domain.repository.UserRepository;
import team23.q_check.identity.domain.service.AuthService;
import team23.q_check.identity.domain.service.AuthService.CodeResult;
import team23.q_check.identity.domain.service.AuthService.TokenResult;
import team23.q_check.identity.domain.service.DiscordOAuthService;
import team23.q_check.identity.domain.service.DiscordOAuthService.DiscordTokenResponse;
import team23.q_check.identity.domain.service.DiscordOAuthService.DiscordUserInfo;
import team23.q_check.identity.dto.SignupRequestDto;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private DiscordOAuthService discordOAuthService;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        discordOAuthService = mock(DiscordOAuthService.class);
        jwtService = mock(JwtService.class);
        authService = new AuthService(userRepository, discordOAuthService, jwtService);
    }

    @Test
    void processCode_existingUser_returnsAccessAndRefreshTokens() throws Exception {
        when(discordOAuthService.exchangeCode("auth-code"))
                .thenReturn(new DiscordTokenResponse("access-1", "Bearer", 3600, "refresh-1", "identify email"));
        when(discordOAuthService.getUserInfo("access-1"))
                .thenReturn(new DiscordUserInfo("discord-1", "kimjyun", "kim@example.com", true));

        User existing = new User("discord-1", "kim@example.com", "kimjyun", "김지윤");
        setId(existing, 7L);
        when(userRepository.findByEmail("kim@example.com")).thenReturn(Optional.of(existing));
        when(jwtService.issueAccessToken(7L)).thenReturn("access-token");
        when(jwtService.issueRefreshToken(7L)).thenReturn("refresh-token");

        CodeResult result = authService.processCode("auth-code");

        assertFalse(result.isNewUser());
        assertEquals("access-token", result.clientToken());
        assertEquals("refresh-token", result.refreshToken());
        assertEquals("refresh-token", existing.getRefreshToken());
    }

    @Test
    void processCode_newUser_returnsSignupToken() throws Exception {
        when(discordOAuthService.exchangeCode("auth-code"))
                .thenReturn(new DiscordTokenResponse("access-1", "Bearer", 3600, "refresh-1", "identify email"));
        when(discordOAuthService.getUserInfo("access-1"))
                .thenReturn(new DiscordUserInfo("discord-1", "kimjyun", "newbie@example.com", true));
        when(userRepository.findByEmail("newbie@example.com")).thenReturn(Optional.empty());
        when(jwtService.issueSignupToken("newbie@example.com", "discord-1", "kimjyun"))
                .thenReturn("signup-token");

        CodeResult result = authService.processCode("auth-code");

        assertTrue(result.isNewUser());
        assertEquals("signup-token", result.clientToken());
        assertNull(result.refreshToken());
    }

    @Test
    void processCode_unverifiedEmail_throwsInvalidRequest() {
        when(discordOAuthService.exchangeCode("auth-code"))
                .thenReturn(new DiscordTokenResponse("access-1", "Bearer", 3600, "refresh-1", "identify email"));
        when(discordOAuthService.getUserInfo("access-1"))
                .thenReturn(new DiscordUserInfo("discord-1", "kimjyun", "kim@example.com", false));

        AppException exception = assertThrows(
                AppException.class,
                () -> authService.processCode("auth-code")
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void processCode_missingEmail_throwsInvalidRequest() {
        when(discordOAuthService.exchangeCode("auth-code"))
                .thenReturn(new DiscordTokenResponse("access-1", "Bearer", 3600, "refresh-1", "identify email"));
        when(discordOAuthService.getUserInfo("access-1"))
                .thenReturn(new DiscordUserInfo("discord-1", "kimjyun", null, true));

        AppException exception = assertThrows(
                AppException.class,
                () -> authService.processCode("auth-code")
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void signup_validToken_createsUserAndReturnsTokens() throws Exception {
        when(jwtService.isSignupToken("signup-token")).thenReturn(true);
        when(jwtService.extractEmail("signup-token")).thenReturn("kim@example.com");
        when(jwtService.extractDiscordId("signup-token")).thenReturn("discord-1");
        when(userRepository.findByEmail("kim@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("kimjyun")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            setId(user, 7L);
            return user;
        });
        when(jwtService.issueAccessToken(7L)).thenReturn("access-token");
        when(jwtService.issueRefreshToken(7L)).thenReturn("refresh-token");

        TokenResult result = authService.signup("signup-token",
                new SignupRequestDto("김지윤", "kimjyun", "kim@example.com"));

        assertEquals(7L, result.userId());
        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("discord-1", captor.getValue().getDiscordId());
        assertEquals("kim@example.com", captor.getValue().getEmail());
        assertEquals("kimjyun", captor.getValue().getUsername());
        assertEquals("김지윤", captor.getValue().getRealName());
    }

    @Test
    void signup_invalidTokenType_throwsUnauthorized() {
        when(jwtService.isSignupToken("not-signup")).thenReturn(false);

        AppException exception = assertThrows(
                AppException.class,
                () -> authService.signup("not-signup",
                        new SignupRequestDto("김지윤", "kimjyun", "kim@example.com"))
        );
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void signup_emailMismatch_throwsInvalidRequest() {
        when(jwtService.isSignupToken("signup-token")).thenReturn(true);
        when(jwtService.extractEmail("signup-token")).thenReturn("kim@example.com");

        AppException exception = assertThrows(
                AppException.class,
                () -> authService.signup("signup-token",
                        new SignupRequestDto("김지윤", "kimjyun", "other@example.com"))
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void signup_emailAlreadyRegistered_throwsConflict() throws Exception {
        when(jwtService.isSignupToken("signup-token")).thenReturn(true);
        when(jwtService.extractEmail("signup-token")).thenReturn("kim@example.com");
        User existing = new User("discord-1", "kim@example.com", "kimjyun", "김지윤");
        setId(existing, 7L);
        when(userRepository.findByEmail("kim@example.com")).thenReturn(Optional.of(existing));

        AppException exception = assertThrows(
                AppException.class,
                () -> authService.signup("signup-token",
                        new SignupRequestDto("김지윤", "kimjyun", "kim@example.com"))
        );
        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    @Test
    void signup_usernameTaken_throwsConflict() {
        when(jwtService.isSignupToken("signup-token")).thenReturn(true);
        when(jwtService.extractEmail("signup-token")).thenReturn("kim@example.com");
        when(userRepository.findByEmail("kim@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("kimjyun")).thenReturn(true);

        AppException exception = assertThrows(
                AppException.class,
                () -> authService.signup("signup-token",
                        new SignupRequestDto("김지윤", "kimjyun", "kim@example.com"))
        );
        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void refresh_validToken_returnsNewTokenPairAndRotates() throws Exception {
        User user = new User("discord-1", "kim@example.com", "kimjyun", "김지윤");
        setId(user, 7L);
        user.updateRefreshToken("old-refresh");

        when(jwtService.isRefreshToken("old-refresh")).thenReturn(true);
        when(jwtService.extractUserId("old-refresh")).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(jwtService.issueAccessToken(7L)).thenReturn("new-access");
        when(jwtService.issueRefreshToken(7L)).thenReturn("new-refresh");

        TokenResult result = authService.refresh("old-refresh");

        assertEquals(7L, result.userId());
        assertEquals("new-access", result.accessToken());
        assertEquals("new-refresh", result.refreshToken());
        assertEquals("new-refresh", user.getRefreshToken());
    }

    @Test
    void refresh_nonRefreshToken_throwsUnauthorized() {
        when(jwtService.isRefreshToken("access-token")).thenReturn(false);

        AppException exception = assertThrows(
                AppException.class,
                () -> authService.refresh("access-token")
        );
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void refresh_storedRefreshTokenMismatch_throwsUnauthorized() throws Exception {
        User user = new User("discord-1", "kim@example.com", "kimjyun", "김지윤");
        setId(user, 7L);
        user.updateRefreshToken("current-refresh");

        when(jwtService.isRefreshToken("stale-refresh")).thenReturn(true);
        when(jwtService.extractUserId("stale-refresh")).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        AppException exception = assertThrows(
                AppException.class,
                () -> authService.refresh("stale-refresh")
        );
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void isUsernameAvailable_reflectsRepositoryExistence() {
        when(userRepository.existsByUsername("free-nick")).thenReturn(false);
        when(userRepository.existsByUsername("taken-nick")).thenReturn(true);

        assertTrue(authService.isUsernameAvailable("free-nick"));
        assertFalse(authService.isUsernameAvailable("taken-nick"));
    }

    private void setId(User user, Long id) throws Exception {
        Field field = User.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }
}
