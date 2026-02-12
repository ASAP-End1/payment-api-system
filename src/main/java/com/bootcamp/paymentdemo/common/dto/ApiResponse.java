package com.bootcamp.paymentdemo.common.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public class ApiResponse<T> {

    private final boolean success;
    private final HttpStatus code;
    private final String message;
    private final T data;

    // 성공
    public static <T> ApiResponse<T> success(HttpStatus status, String message, T data) {
        return new ApiResponse<>(true, status, message, data);
    }

    // 실패
    public static ApiResponse<Void> error(HttpStatus status, String message) {
        return new ApiResponse<>(false, status, message, null);
    }

    // 실패 (입력값 검증)
    public static <T> ApiResponse<T> error(HttpStatus status, String message, T data) {
        return new ApiResponse<>(false, status, message, data);
    }
}