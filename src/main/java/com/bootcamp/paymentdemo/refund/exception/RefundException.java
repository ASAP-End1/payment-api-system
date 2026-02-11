package com.bootcamp.paymentdemo.refund.exception;

import com.bootcamp.paymentdemo.refund.consts.ErrorEnum;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RefundException extends RuntimeException {
    private final HttpStatus httpStatus;

    public RefundException(ErrorEnum errorEnum) {
        super(errorEnum.getMessage());
        this.httpStatus = errorEnum.getStatus();
    }
}
