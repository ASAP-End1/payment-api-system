package com.bootcamp.paymentdemo.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ServiceException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public ServiceException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }
}