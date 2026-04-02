package team23.q_check.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
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
@RequestMapping("/auth")
public class AuthController {

    private static final String OAUTH_STATE_SESSION_KEY = "oauth2_state";

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @Operation(summary = "Discord 로그인 리다이렉트", description = "Discord OAuth2 인증 페이지로 리다이렉트합니다")
    @GetMapping("/login")
    public void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String state = UUID.randomUUID().toString();
        request.getSession(true).setAttribute(OAUTH_STATE_SESSION_KEY, state);
        response.sendRedirect(authService.getAuthorizationUrl(state));
    }

    @Operation(
            summary = "Discord 인가코드 콜백",
            description = "Discord에서 전달받은 인가코드를 처리합니다. " +
                    "기존 회원이면 access token(+ refresh token 쿠키)을, 신규 회원이면 10분짜리 signup JWT를 반환합니다."
    )
    @GetMapping("/code")
    public ApiResponse<AuthCodeResponseDto> handleCode(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        validateOAuthState(request, state);
        CodeResult result = authService.processCode(code);
        if (!result.isNewUser()) {
            setRefreshCookie(response, result.refreshToken());
        }
        return ApiResponse.ok(new AuthCodeResponseDto(result.isNewUser(), result.clientToken()));
    }

    @Operation(summary = "닉네임 중복 확인")
    @GetMapping("/nickname/check")
    public ApiResponse<NicknameCheckResponseDto> checkNickname(
            @Parameter(description = "확인할 닉네임") @RequestParam String nickname
    ) {
        return ApiResponse.ok(new NicknameCheckResponseDto(authService.isNicknameAvailable(nickname)));
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

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofMillis(jwtProperties.refreshTokenExpiration()))
                .sameSite("Strict")
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
