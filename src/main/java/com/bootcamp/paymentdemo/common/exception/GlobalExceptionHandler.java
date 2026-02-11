package com.bootcamp.paymentdemo.common.exception;

import com.bootcamp.paymentdemo.common.dto.ErrorResponse;
import com.bootcamp.paymentdemo.membership.exception.MembershipNotFoundException;
import com.bootcamp.paymentdemo.membership.exception.UserPaidAmountNotFoundException;
import com.bootcamp.paymentdemo.refund.exception.PortOneException;
import com.bootcamp.paymentdemo.refund.exception.RefundException;
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

    // 모든 예외 ServiceException 하나로 처리
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(ServiceException e) {
        log.warn("{}: {}", e.getClass().getSimpleName(), e.getMessage());
        ErrorResponse error = new ErrorResponse(e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(error);
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

    @ExceptionHandler(RefundException.class)
    public ResponseEntity<ErrorResponse> handleRefundException(RefundException e) {
        log.warn("RefundException: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse(e.getHttpStatus().name(), e.getMessage());
        return ResponseEntity.status(e.getHttpStatus()).body(error);
    }

    @ExceptionHandler(PortOneException.class)
    public  ResponseEntity<ErrorResponse> handlePortOneException(PortOneException e) {
        log.warn("PortOneException: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse(e.getHttpStatus().name(), e.getMessage());
        return ResponseEntity.status(e.getHttpStatus()).body(error);
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

    // 특정 멤버십 등급이 존재하지 않는 경우
    @ExceptionHandler(MembershipNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMembershipNotFoundException(MembershipNotFoundException e) {
        log.error("MembershipNotFoundException: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("MEMBERSHIP_NOT_FOUND", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // 사용자의 총 결제 금액 정보가 존재하지 않는 경우
    @ExceptionHandler(UserPaidAmountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserPaidAmountNotFoundException(UserPaidAmountNotFoundException e) {
        log.error("UserPaidAmountNotFoundException: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("USER_PAID_AMOUNT_NOT_FOUND", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
