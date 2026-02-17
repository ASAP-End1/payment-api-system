package com.bootcamp.paymentdemo.refund.dto;

import lombok.*;

@Getter
public class RefundResponse {

    private final Long orderId;
    private final String orderNumber;

    public RefundResponse(Long orderId, String orderNumber) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
    }
}
