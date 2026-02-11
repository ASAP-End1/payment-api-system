package com.bootcamp.paymentdemo.user.exception;

import com.bootcamp.paymentdemo.common.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends ServiceException {
    public DuplicateEmailException(String message) {
        super("DUPLICATE_EMAIL", message, HttpStatus.CONFLICT);
    }
}
