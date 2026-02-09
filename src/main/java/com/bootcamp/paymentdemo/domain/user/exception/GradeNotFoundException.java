package com.bootcamp.paymentdemo.domain.user.exception;

public class GradeNotFoundException extends RuntimeException {
    public GradeNotFoundException(String message) {
        super(message);
    }
}
