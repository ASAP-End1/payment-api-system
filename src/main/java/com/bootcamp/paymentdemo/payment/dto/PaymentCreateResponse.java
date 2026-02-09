package com.bootcamp.paymentdemo.payment.dto;

import lombok.Getter;

@Getter
public class PaymentCreateResponse {
    private final boolean success;
    private final String paymentId;
    private final String status;

    public PaymentCreateResponse(boolean success, String paymentId, String status) {
        this.success = success;
        this.paymentId = paymentId;
        this.status = status;
    }

}