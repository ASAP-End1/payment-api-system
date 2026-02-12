package com.bootcamp.paymentdemo.payment.dto;

import lombok.Getter;

@Getter
public class PaymentConfirmResponse {
    private final Long orderId;
    private final String orderNumber;

    public PaymentConfirmResponse(Long orderId, String orderNumber) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
    }

}