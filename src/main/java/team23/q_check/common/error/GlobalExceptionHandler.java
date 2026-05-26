package team23.q_check.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import team23.q_check.common.response.ApiResponse;

import java.nio.file.AccessDeniedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException e) {
        ErrorCode code = e.getErrorCode();
        if (code.getHttpStatus().is5xxServerError()) {
            log.error("app.exception code={} msg={}", code.getCode(), e.getMessage(), e);
        } else {
            log.warn("app.exception code={} msg={}", code.getCode(), e.getMessage());
        }
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.error(code, e.getMessage()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(Exception e) {
        log.info("request.invalid type={} msg={}", e.getClass().getSimpleName(), e.getMessage());
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST, e.getMessage()));
    }

    /**
     * 필수 헤더 누락은 기본적으로 입력 검증 실패(400). 단, Authorization 헤더는
     * 인증 컨텍스트라 401로 매핑해 클라이언트가 로그인 흐름으로 분기할 수 있게 한다.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException e) {
        ErrorCode errorCode = HttpHeaders.AUTHORIZATION.equalsIgnoreCase(e.getHeaderName())
                ? ErrorCode.UNAUTHORIZED
                : ErrorCode.INVALID_REQUEST;
        log.info("request.missing_header header={} code={}", e.getHeaderName(), errorCode.getCode());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode, e.getMessage()));
    }

    /**
     * 필수 쿠키 누락은 401로 매핑한다. 현재 유일한 필수 쿠키는 refresh_token 이고,
     * 그 부재는 곧 "갱신할 세션 없음"을 뜻하므로 자동 로그인 시도가 catch-all 500이 아니라
     * 401로 떨어져 클라이언트가 로그인 흐름으로 분기할 수 있게 한다.
     */
    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingCookie(MissingRequestCookieException e) {
        log.info("request.missing_cookie cookie={} code={}", e.getCookieName(), ErrorCode.UNAUTHORIZED.getCode());
        return ResponseEntity
                .status(ErrorCode.UNAUTHORIZED.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.UNAUTHORIZED, e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("access.denied msg={}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.FORBIDDEN.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.FORBIDDEN, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("unhandled.exception type={} msg={}", e.getClass().getName(), e.getMessage(), e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR, e.getMessage()));
    }
}
