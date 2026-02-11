package com.bootcamp.paymentdemo.membership.exception;

public class UserPaidAmountNotFoundException extends RuntimeException {
    public UserPaidAmountNotFoundException(String message) {
        super(message);
    }
}
