package team23.q_check.common.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import team23.q_check.common.response.ApiResponse;

import java.nio.file.AccessDeniedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException e) {
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(Exception e) {
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity
                .status(ErrorCode.FORBIDDEN.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.FORBIDDEN, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR, e.getMessage()));
    }
}
