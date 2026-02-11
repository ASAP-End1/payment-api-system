package com.bootcamp.paymentdemo.user.exception;

import com.bootcamp.paymentdemo.common.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class GradeNotFoundException extends ServiceException {
    public GradeNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
