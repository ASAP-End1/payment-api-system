package com.bootcamp.paymentdemo.membership.exception;

import com.bootcamp.paymentdemo.common.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class UserPaidAmountNotFoundException extends ServiceException {
    public UserPaidAmountNotFoundException(String message) {
        super("USER_PAID_AMOUNT_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
