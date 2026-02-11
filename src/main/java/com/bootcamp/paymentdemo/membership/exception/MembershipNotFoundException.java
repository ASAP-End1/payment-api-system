package com.bootcamp.paymentdemo.membership.exception;

import com.bootcamp.paymentdemo.common.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class MembershipNotFoundException extends ServiceException {
    public MembershipNotFoundException(String message) {
        super("MEMBERSHIP_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
