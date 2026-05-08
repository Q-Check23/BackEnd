package team23.q_check.identity.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team23.q_check.common.auth.JwtService;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.identity.domain.model.User;
import team23.q_check.identity.dto.SignupRequestDto;
import team23.q_check.identity.domain.repository.UserRepository;
import team23.q_check.identity.domain.service.DiscordOAuthService.DiscordUserInfo;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final DiscordOAuthService discordOAuthService;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, DiscordOAuthService discordOAuthService, JwtService jwtService) {
        this.userRepository = userRepository;
        this.discordOAuthService = discordOAuthService;
        this.jwtService = jwtService;
    }

    /**
     * Discord 인가코드를 처리하여 기존 회원이면 토큰을, 신규 회원이면 회원가입용 JWT를 반환합니다.
     */
    @Transactional
    public CodeResult processCode(String code) {
        DiscordOAuthService.DiscordTokenResponse discordToken = discordOAuthService.exchangeCode(code);
        DiscordUserInfo userInfo = discordOAuthService.getUserInfo(discordToken.accessToken());

        if (userInfo.email() == null || !Boolean.TRUE.equals(userInfo.verified())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Discord 계정에 인증된 이메일이 필요합니다");
        }

        Optional<User> existing = userRepository.findByEmail(userInfo.email());
        if (existing.isPresent()) {
            User user = existing.get();
            String accessToken = jwtService.issueAccessToken(user.getId());
            String refreshToken = jwtService.issueRefreshToken(user.getId());
            user.updateRefreshToken(refreshToken);
            return new CodeResult(false, accessToken, refreshToken);
        }

        String signupToken = jwtService.issueSignupToken(userInfo.email(), userInfo.id(), userInfo.username());
        return new CodeResult(true, signupToken, null);
    }

    /**
     * 회원가입을 처리합니다. signup JWT 검증 후 사용자를 생성하고 인증 토큰을 반환합니다.
     */
    @Transactional
    public TokenResult signup(String signupToken, SignupRequestDto request) {
        if (!jwtService.isSignupToken(signupToken)) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "유효하지 않은 회원가입 토큰입니다");
        }

        String emailFromToken = jwtService.extractEmail(signupToken);
        if (!emailFromToken.equals(request.email())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "이메일이 회원가입 토큰의 이메일과 일치하지 않습니다");
        }

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new AppException(ErrorCode.CONFLICT, "이미 가입된 이메일입니다");
        }

        if (userRepository.existsByUsername(request.nickname())) {
            throw new AppException(ErrorCode.CONFLICT, "이미 사용 중인 닉네임입니다");
        }

        String discordId = jwtService.extractDiscordId(signupToken);
        User user = new User(discordId, request.email(), request.nickname(), request.name());
        userRepository.save(user);

        String accessToken = jwtService.issueAccessToken(user.getId());
        String refreshToken = jwtService.issueRefreshToken(user.getId());
        user.updateRefreshToken(refreshToken);

        return new TokenResult(user.getId(), accessToken, refreshToken);
    }

    /**
     * refresh token으로 새로운 access/refresh token을 발급합니다.
     */
    @Transactional
    public TokenResult refresh(String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "유효하지 않은 refresh token입니다");
        }

        Long userId = jwtService.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Refresh token이 일치하지 않습니다");
        }

        String newAccessToken = jwtService.issueAccessToken(userId);
        String newRefreshToken = jwtService.issueRefreshToken(userId);
        user.updateRefreshToken(newRefreshToken);

        return new TokenResult(userId, newAccessToken, newRefreshToken);
    }

    public String getAuthorizationUrl(String state) {
        return discordOAuthService.getAuthorizationUrl(state);
    }

    /**
     * 로그아웃 — 사용자의 refresh token을 무효화한다.
     * (access token 자체는 stateless라 만료까지 기다림)
     */
    @Transactional
    public void logout(Long userId) {
        userRepository.findById(userId).ifPresent(user -> user.updateRefreshToken(null));
    }

    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByUsername(nickname);
    }

    public record CodeResult(boolean isNewUser, String clientToken, String refreshToken) {}
    public record TokenResult(Long userId, String accessToken, String refreshToken) {}
}
