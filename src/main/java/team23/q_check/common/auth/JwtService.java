package team23.q_check.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import team23.q_check.common.config.JwtProperties;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_DISCORD_ID = "discordId";
    private static final String CLAIM_DISCORD_USERNAME = "discordUsername";
    static final String TYPE_ACCESS = "access";
    static final String TYPE_REFRESH = "refresh";
    static final String TYPE_SIGNUP = "signup";

    private final SecretKey key;
    private final JwtProperties props;

    public JwtService(JwtProperties props) {
        this.props = props;
        // JWT_SECRET 환경변수는 최소 32자(256 bit) 이상이어야 합니다
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(Long userId) {
        return buildToken(String.valueOf(userId), TYPE_ACCESS, props.accessTokenExpiration());
    }

    public String issueRefreshToken(Long userId) {
        return buildToken(String.valueOf(userId), TYPE_REFRESH, props.refreshTokenExpiration());
    }

    public String issueSignupToken(String email, String discordId, String discordUsername) {
        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_TYPE, TYPE_SIGNUP)
                .claim(CLAIM_DISCORD_ID, discordId)
                .claim(CLAIM_DISCORD_USERNAME, discordUsername)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + props.signupTokenExpiration()))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Token has expired");
        } catch (JwtException e) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Invalid token");
        }
    }

    /** access 또는 refresh 토큰에서 userId 추출 */
    public Long extractUserId(String token) {
        Claims claims = parseToken(token);
        validateType(claims, TYPE_ACCESS, TYPE_REFRESH);
        return Long.parseLong(claims.getSubject());
    }

    /** signup 토큰에서 이메일 추출 */
    public String extractEmail(String signupToken) {
        Claims claims = parseToken(signupToken);
        validateType(claims, TYPE_SIGNUP);
        return claims.getSubject();
    }

    public String extractDiscordId(String signupToken) {
        return parseToken(signupToken).get(CLAIM_DISCORD_ID, String.class);
    }

    public boolean isSignupToken(String token) {
        return TYPE_SIGNUP.equals(parseToken(token).get(CLAIM_TYPE));
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(parseToken(token).get(CLAIM_TYPE));
    }

    private String buildToken(String subject, String type, long expirationMs) {
        return Jwts.builder()
                .subject(subject)
                .claim(CLAIM_TYPE, type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    private void validateType(Claims claims, String... allowed) {
        String type = claims.get(CLAIM_TYPE, String.class);
        for (String a : allowed) {
            if (a.equals(type)) return;
        }
        throw new AppException(ErrorCode.UNAUTHORIZED, "Invalid token type");
    }
}
