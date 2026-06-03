package team23.q_check.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;
import team23.q_check.common.config.FrontendProperties;
import team23.q_check.common.config.JwtProperties;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.common.response.ApiResponse;
import team23.q_check.identity.dto.*;
import team23.q_check.identity.domain.service.AuthService;
import team23.q_check.identity.domain.service.AuthService.CodeResult;
import team23.q_check.identity.domain.service.AuthService.TokenResult;

import java.io.IOException;
import java.time.Duration;

@Tag(name = "Auth", description = "Discord OAuth2 인증 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String OAUTH_STATE_SESSION_KEY = "oauth2_state";
    // 가장 최근 OAuth 시도가 prompt=none(silent) 이었는지. 콜백에서 Discord 가 error 로
    // 돌려보냈을 때 일반 동의 흐름으로 자동 재시도할지 판단하는 가드.
    private static final String OAUTH_SILENT_SESSION_KEY = "oauth2_silent";

    private final AuthService authService;
    private final JwtProperties jwtProperties;
    private final FrontendProperties frontendProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties, FrontendProperties frontendProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
        this.frontendProperties = frontendProperties;
    }

    @Operation(
            summary = "Discord 로그인 리다이렉트",
            description = "Discord OAuth2 인증 페이지로 리다이렉트합니다. " +
                    "silent=true 면 prompt=none 을 붙여 이미 인가된 사용자의 동의 화면을 건너뜁니다. " +
                    "Discord 가 error 로 응답하면 자동으로 일반 흐름으로 재시도합니다."
    )
    @GetMapping("/login")
    public void login(
            @RequestParam(required = false) Boolean silent,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        initiateOAuth(request, response, Boolean.TRUE.equals(silent));
    }

    @Operation(
            summary = "Discord 인가코드 콜백",
            description = "Discord 가 브라우저를 이 URL 로 redirect 하면 백엔드가 인가코드를 처리한 뒤, " +
                    "프론트의 콜백 라우트(FRONTEND_AUTH_CALLBACK_URL)로 302 redirect 합니다. " +
                    "기존 회원: ?token=<accessJwt>&isNewUser=false (+ refresh token httpOnly 쿠키). " +
                    "신규 회원: ?token=<signupJwt>&isNewUser=true. " +
                    "에러: ?error=<코드>&message=<설명>. " +
                    "silent 시도가 Discord 에서 error 로 돌아오면 일반 흐름으로 자동 재시도합니다."
    )
    @GetMapping("/code")
    public void handleCode(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        // Discord 가 error 로 응답한 경우: silent 시도였다면 일반 흐름으로 자동 재시도,
        // 아니면 프론트에 에러 전달
        if (error != null) {
            boolean wasSilent = consumeSilentFlag(request);
            if (wasSilent) {
                log.info("auth.code.silent_failed error={} retrying with consent", error);
                initiateOAuth(request, response, false);
                return;
            }
            log.warn("auth.code.discord_error error={}", error);
            response.sendRedirect(buildErrorUrl("OAUTH_ERROR", "Discord 인증에 실패했습니다"));
            return;
        }

        if (code == null) {
            response.sendRedirect(buildErrorUrl("INVALID_REQUEST", "code 파라미터가 필요합니다"));
            return;
        }

        try {
            validateOAuthState(request, state);
            consumeSilentFlag(request);
            CodeResult result = authService.processCode(code);
            if (!result.isNewUser()) {
                setRefreshCookie(response, result.refreshToken());
            }
            response.sendRedirect(buildSuccessUrl(result));
        } catch (AppException e) {
            log.warn("auth.code.error code={} msg={}", e.getErrorCode().getCode(), e.getMessage());
            response.sendRedirect(buildErrorUrl(e.getErrorCode().getCode(), e.getMessage()));
        }
    }

    private void initiateOAuth(HttpServletRequest request, HttpServletResponse response, boolean silent) throws IOException {
        String state = UUID.randomUUID().toString();
        HttpSession session = request.getSession(true);
        session.setAttribute(OAUTH_STATE_SESSION_KEY, state);
        if (silent) {
            session.setAttribute(OAUTH_SILENT_SESSION_KEY, Boolean.TRUE);
        } else {
            session.removeAttribute(OAUTH_SILENT_SESSION_KEY);
        }
        response.sendRedirect(authService.getAuthorizationUrl(state, silent));
    }

    private boolean consumeSilentFlag(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        Object flag = session.getAttribute(OAUTH_SILENT_SESSION_KEY);
        session.removeAttribute(OAUTH_SILENT_SESSION_KEY);
        return Boolean.TRUE.equals(flag);
    }

    private String buildSuccessUrl(CodeResult result) {
        return UriComponentsBuilder.fromUriString(frontendProperties.authCallbackUrl())
                .queryParam("token", result.clientToken())
                .queryParam("isNewUser", result.isNewUser())
                .build()
                .encode()
                .toUriString();
    }

    private String buildErrorUrl(String errorCode, String message) {
        return UriComponentsBuilder.fromUriString(frontendProperties.authCallbackUrl())
                .queryParam("error", errorCode)
                .queryParam("message", message)
                .build()
                .encode()
                .toUriString();
    }

    @Operation(summary = "아이디 중복 확인")
    @GetMapping("/username/check")
    public ApiResponse<UsernameCheckResponseDto> checkUsername(
            @Parameter(description = "확인할 아이디") @RequestParam String username
    ) {
        return ApiResponse.ok(new UsernameCheckResponseDto(authService.isUsernameAvailable(username)));
    }

    @Operation(
            summary = "회원가입",
            description = "Authorization 헤더에 signup JWT를 포함해 요청합니다. " +
                    "성공 시 access token을 반환하고 refresh token을 쿠키에 설정합니다."
    )
    @PostMapping("/signup")
    public ApiResponse<AuthTokenResponseDto> signup(
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody SignupRequestDto request,
            HttpServletResponse response
    ) {
        String signupToken = extractBearerToken(bearerToken);
        TokenResult result = authService.signup(signupToken, request);
        setRefreshCookie(response, result.refreshToken());
        return ApiResponse.ok(new AuthTokenResponseDto(result.userId(), result.accessToken()));
    }

    @Operation(
            summary = "Access Token 갱신",
            description = "refresh_token 쿠키를 사용하여 새로운 access token과 refresh token을 발급합니다."
    )
    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponseDto> refresh(
            @CookieValue("refresh_token") String refreshToken,
            HttpServletResponse response
    ) {
        TokenResult result = authService.refresh(refreshToken);
        setRefreshCookie(response, result.refreshToken());
        return ApiResponse.ok(new AuthTokenResponseDto(result.userId(), result.accessToken()));
    }

    @Operation(
            summary = "로그아웃",
            description = "서버 측 refresh token을 무효화하고 refresh_token 쿠키를 즉시 만료시킵니다."
    )
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @team23.q_check.common.auth.CurrentUserId Long currentUserId,
            HttpServletResponse response
    ) {
        authService.logout(currentUserId);
        ResponseCookie expired = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expired.toString());
        return ApiResponse.ok(null);
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofMillis(jwtProperties.refreshTokenExpiration()))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void validateOAuthState(HttpServletRequest request, String state) {
        HttpSession session = request.getSession(false);
        String stored = (session != null) ? (String) session.getAttribute(OAUTH_STATE_SESSION_KEY) : null;
        if (session != null) {
            session.removeAttribute(OAUTH_STATE_SESSION_KEY);
        }
        if (stored == null || !stored.equals(state)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "OAuth state가 유효하지 않습니다");
        }
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Authorization 헤더가 없거나 형식이 올바르지 않습니다");
        }
        return authHeader.substring(7);
    }
}
