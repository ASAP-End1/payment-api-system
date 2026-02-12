package com.bootcamp.paymentdemo.external.portone.exception;

import com.bootcamp.paymentdemo.common.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class PortOneException  extends ServiceException {
    public PortOneException (HttpStatus status, String message) {
        super(message, status);
    }
}
