package com.bootcamp.paymentdemo.external.portone.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PortOneException  extends RuntimeException {

    private final HttpStatus httpStatus;

    public PortOneException (HttpStatus type, String message) {

        super(message);
        this.httpStatus = type;
    }
}
