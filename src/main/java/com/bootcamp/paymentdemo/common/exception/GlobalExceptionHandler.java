package com.bootcamp.paymentdemo.common.exception;

import com.bootcamp.paymentdemo.common.dto.ErrorResponse;
import com.bootcamp.paymentdemo.point.exception.EarnedPointNotFoundException;
import com.bootcamp.paymentdemo.user.exception.DuplicateEmailException;
import com.bootcamp.paymentdemo.user.exception.GradeNotFoundException;
import com.bootcamp.paymentdemo.user.exception.InvalidCredentialsException;
import com.bootcamp.paymentdemo.user.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 이메일 중복
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmailException(DuplicateEmailException e) {
        log.warn("DuplicateEmailException: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("DUPLICATE_EMAIL", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // 이메일 or 비밀번호 불일치
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(InvalidCredentialsException e) {
        log.warn("InvalidCredentialsException: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("INVALID_CREDENTIALS", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // 존재하지 않는 사용자
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException e) {
        log.warn("UserNotFoundException: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("USER_NOT_FOUND", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // 사용자의 멤버십 등급 찾지 못하는 경우
    @ExceptionHandler(GradeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGradeNotFoundException(GradeNotFoundException e) {
        log.error("GradeNotFoundException: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("GRADE_NOT_FOUND", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // 입력값 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("MethodArgumentNotValidException: {}", e.getMessage());

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("code", "VALIDATION_ERROR");
        response.put("message", "입력값 검증에 실패했습니다.");
        response.put("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("BAD_REQUEST", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected exception: ", e);
        ErrorResponse error = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // 존재하지 않는 적립금
    @ExceptionHandler(EarnedPointNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEarnedPointNotFoundException(EarnedPointNotFoundException e) {
        log.warn("EarnedPointNotFoundException: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("EARNED_POINT_NOT_FOUND", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
