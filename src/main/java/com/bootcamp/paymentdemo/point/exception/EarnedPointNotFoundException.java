package com.bootcamp.paymentdemo.point.exception;

public class EarnedPointNotFoundException extends RuntimeException{
    public EarnedPointNotFoundException(String message) {
        super(message);
    }
}
