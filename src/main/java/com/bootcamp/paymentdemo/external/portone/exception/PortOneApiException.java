package com.bootcamp.paymentdemo.external.portone.exception;

public class PortOneApiException extends RuntimeException {
    public PortOneApiException(String type, String message, int status) {
        super(String.format("[%d %s] %s", status, type, message));
    }
}
