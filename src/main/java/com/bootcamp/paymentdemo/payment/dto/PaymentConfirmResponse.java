package com.bootcamp.paymentdemo.payment.dto;

import lombok.Getter;

@Getter
public class PaymentConfirmResponse {
    private final boolean success;
    private final Long orderId;
    private final String orderNumber;
    private final String status;

    public PaymentConfirmResponse(boolean success, Long orderId, String orderNumber, String status) {
        this.success = success;
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.status = status;
    }

}