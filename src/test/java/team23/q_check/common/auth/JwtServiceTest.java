package team23.q_check.common.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import team23.q_check.common.config.JwtProperties;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    /** HMAC-SHA256 최소 32자(256bit). */
    private static final String SECRET = "test-secret-key-of-at-least-32-bytes-please";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, 3_600_000L, 604_800_000L, 600_000L));
    }

    @Test
    void issueAccessToken_extractAccessTokenUserId_roundTrips() {
        String token = jwtService.issueAccessToken(7L);

        assertEquals(7L, jwtService.extractAccessTokenUserId(token));
    }

    @Test
    void issueRefreshToken_extractAccessTokenUserId_rejectsNonAccessTypeWithUnauthorized() {
        String refresh = jwtService.issueRefreshToken(7L);

        AppException exception = assertThrows(
                AppException.class,
                () -> jwtService.extractAccessTokenUserId(refresh)
        );
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void extractUserId_acceptsAccessOrRefresh() {
        String access = jwtService.issueAccessToken(7L);
        String refresh = jwtService.issueRefreshToken(7L);

        assertEquals(7L, jwtService.extractUserId(access));
        assertEquals(7L, jwtService.extractUserId(refresh));
    }

    @Test
    void extractUserId_rejectsSignupToken() {
        String signup = jwtService.issueSignupToken("kim@example.com", "discord-1", "kimjyun");

        AppException exception = assertThrows(
                AppException.class,
                () -> jwtService.extractUserId(signup)
        );
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void issueSignupToken_extractsEmailAndDiscordId() {
        String signup = jwtService.issueSignupToken("kim@example.com", "discord-1", "kimjyun");

        assertEquals("kim@example.com", jwtService.extractEmail(signup));
        assertEquals("discord-1", jwtService.extractDiscordId(signup));
        assertTrue(jwtService.isSignupToken(signup));
    }

    @Test
    void extractEmail_rejectsNonSignupToken() {
        String access = jwtService.issueAccessToken(7L);

        AppException exception = assertThrows(
                AppException.class,
                () -> jwtService.extractEmail(access)
        );
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void isSignupToken_andIsRefreshToken_classifyCorrectly() {
        String access = jwtService.issueAccessToken(7L);
        String refresh = jwtService.issueRefreshToken(7L);
        String signup = jwtService.issueSignupToken("kim@example.com", "discord-1", "kimjyun");

        assertFalse(jwtService.isSignupToken(access));
        assertFalse(jwtService.isSignupToken(refresh));
        assertTrue(jwtService.isSignupToken(signup));
        assertTrue(jwtService.isRefreshToken(refresh));
        assertFalse(jwtService.isRefreshToken(access));
    }

    @Test
    void parseToken_withGarbledToken_throwsUnauthorized() {
        AppException exception = assertThrows(
                AppException.class,
                () -> jwtService.parseToken("not-a-real-jwt")
        );
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void parseToken_signedWithDifferentSecret_throwsUnauthorized() {
        JwtService other = new JwtService(
                new JwtProperties("a-completely-different-secret-of-32-bytes!", 3_600_000L, 604_800_000L, 600_000L));
        String foreignToken = other.issueAccessToken(7L);

        AppException exception = assertThrows(
                AppException.class,
                () -> jwtService.parseToken(foreignToken)
        );
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void parseToken_expiredToken_throwsUnauthorized() throws Exception {
        // 만료까지 1ms — 발급 직후 충분히 시간이 흐른 뒤 검증되도록 함
        JwtService shortLived = new JwtService(new JwtProperties(SECRET, 1L, 1L, 1L));
        String token = shortLived.issueAccessToken(7L);
        Thread.sleep(20L);

        AppException exception = assertThrows(
                AppException.class,
                () -> shortLived.parseToken(token)
        );
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }
}
