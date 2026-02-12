package com.bootcamp.paymentdemo.external.portone.exception;

import com.bootcamp.paymentdemo.common.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class PortOneApiException extends ServiceException {
    public PortOneApiException(String type, String message, int status) {
        super(String.format("[%d %s] %s", status, type, message), HttpStatus.valueOf(status));
    }
}
