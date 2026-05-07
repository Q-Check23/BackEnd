package team23.q_check.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.common.log.RequestLoggingFilter;
import team23.q_check.common.response.ApiResponse;

import java.io.IOException;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthInterceptor.class);

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final boolean devAuthEnabled;

    public JwtAuthInterceptor(
            JwtService jwtService,
            ObjectMapper objectMapper,
            @Value("${dev-auth.enabled:false}") boolean devAuthEnabled
    ) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.devAuthEnabled = devAuthEnabled;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // CORS preflight 요청은 통과
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                Long userId = jwtService.extractAccessTokenUserId(authHeader.substring(7)); // access token만 허용
                MDC.put(RequestLoggingFilter.MDC_USER_ID, String.valueOf(userId));
                return true;
            }

            // 개발용 X-USER-ID fallback — dev-auth.enabled=true 일 때만 허용
            if (devAuthEnabled) {
                String devHeader = request.getHeader("X-USER-ID");
                if (devHeader != null && !devHeader.isBlank()) {
                    try {
                        Long userId = Long.parseLong(devHeader);
                        MDC.put(RequestLoggingFilter.MDC_USER_ID, String.valueOf(userId));
                        log.warn("auth.dev_fallback used userId={} path={} — 프로덕션이라면 dev-auth.enabled=false 확인 필요",
                                userId, request.getRequestURI());
                        return true;
                    } catch (NumberFormatException e) {
                        log.warn("auth.dev_fallback invalid value='{}'", devHeader);
                        writeError(response, ErrorCode.INVALID_REQUEST, "X-USER-ID must be a number");
                        return false;
                    }
                }
            }

            log.info("auth.missing path={}", request.getRequestURI());
            writeError(response, ErrorCode.UNAUTHORIZED, "로그인이 필요합니다");
            return false;

        } catch (AppException e) {
            log.info("auth.rejected reason={} path={}", e.getMessage(), request.getRequestURI());
            writeError(response, e.getErrorCode(), e.getMessage());
            return false;
        }
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.error(errorCode, message))
        );
    }
}
