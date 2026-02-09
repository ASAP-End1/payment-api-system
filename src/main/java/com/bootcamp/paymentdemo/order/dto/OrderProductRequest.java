package com.bootcamp.paymentdemo.order.dto;

import lombok.Getter;

@Getter
public class OrderProductRequest {

    private Long productId;
    private int count;

    public OrderProductRequest(Long productId, int count) {
        this.productId = productId;
        this.count = count;
    }
}
