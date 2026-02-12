package com.bootcamp.paymentdemo.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class OrderProductRequest {

    private Long productId;

    @JsonProperty("quantity")
    private int count;

    public OrderProductRequest(Long productId, int count) {
        this.productId = productId;
        this.count = count;
    }
}
