package team23.q_check.common.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;

@Component
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String USER_ID_HEADER = "X-USER-ID";

    private final JwtService jwtService;

    public CurrentUserIdArgumentResolver(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && (Long.class.equals(parameter.getParameterType()) || long.class.equals(parameter.getParameterType()));
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Request context is not available");
        }

        // JWT Bearer 토큰 우선 확인 (프로덕션)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtService.extractUserId(token);
        }

        // 개발용 X-USER-ID 헤더 fallback
        String headerValue = request.getHeader(USER_ID_HEADER);
        if (headerValue == null || headerValue.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Missing authentication");
        }
        try {
            return Long.parseLong(headerValue);
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "X-USER-ID must be a number");
        }
    }
}
