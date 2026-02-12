package com.bootcamp.paymentdemo.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class OrderCreateRequest {

    private Long userId;
    private BigDecimal usedPoints;
    @JsonProperty("items")
    private List<OrderProductRequest> orderItems;

    public OrderCreateRequest(Long userId, BigDecimal usedPoints, List<OrderProductRequest> orderItems) {
        this.userId = userId;
        this.usedPoints = usedPoints;
        this.orderItems = orderItems;
    }
}
