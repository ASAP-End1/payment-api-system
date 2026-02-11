package com.bootcamp.paymentdemo.membership.exception;

public class MembershipNotFoundException extends RuntimeException {
    public MembershipNotFoundException(String message) {
        super(message);
    }
}
