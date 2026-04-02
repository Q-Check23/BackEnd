package team23.q_check.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
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
                jwtService.extractAccessTokenUserId(authHeader.substring(7)); // access token만 허용
                return true;
            }

            // 개발용 X-USER-ID fallback — dev-auth.enabled=true 일 때만 허용
            if (devAuthEnabled) {
                String devHeader = request.getHeader("X-USER-ID");
                if (devHeader != null && !devHeader.isBlank()) {
                    try {
                        Long.parseLong(devHeader);
                        return true;
                    } catch (NumberFormatException e) {
                        log.error("[DevAuth] X-USER-ID 파싱 실패: '{}'", devHeader);
                        writeError(response, ErrorCode.INVALID_REQUEST, "X-USER-ID must be a number");
                        return false;
                    }
                }
            }

            writeError(response, ErrorCode.UNAUTHORIZED, "로그인이 필요합니다");
            return false;

        } catch (AppException e) {
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
