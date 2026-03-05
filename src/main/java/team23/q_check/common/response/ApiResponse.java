package team23.q_check.common.response;

import team23.q_check.common.error.ErrorCode;

import java.time.LocalDateTime;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "SUCCESS", "Request succeeded", data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, "SUCCESS", message, data, LocalDateTime.now());
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null, LocalDateTime.now());
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, errorCode.getCode(), message, null, LocalDateTime.now());
    }
}
