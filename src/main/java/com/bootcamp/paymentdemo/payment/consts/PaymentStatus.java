package com.bootcamp.paymentdemo.payment.consts;

import lombok.Getter;

@Getter
public enum PaymentStatus {

    PENDING("PENDING"),
    PAID("PAID"),
    FAIL("FAIL"),
    REFUND("REFUND");

    private final String statusName;

    PaymentStatus(String statusName) {
        this.statusName = statusName;
    }

}
