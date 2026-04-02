package team23.q_check.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.common.response.ApiResponse;

import java.io.IOException;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthInterceptor(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
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
                jwtService.extractUserId(authHeader.substring(7)); // 유효성 검사
                return true;
            }

            // 개발용 X-USER-ID fallback
            String devHeader = request.getHeader("X-USER-ID");
            if (devHeader != null && !devHeader.isBlank()) {
                Long.parseLong(devHeader); // 숫자 유효성 검사
                return true;
            }

            writeError(response, ErrorCode.UNAUTHORIZED, "로그인이 필요합니다");
            return false;

        } catch (AppException e) {
            writeError(response, e.getErrorCode(), e.getMessage());
            return false;
        } catch (NumberFormatException e) {
            writeError(response, ErrorCode.INVALID_REQUEST, "X-USER-ID must be a number");
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
