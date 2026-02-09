package com.bootcamp.paymentdemo.order.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class OrderGetResponse {

    private final Long id;
    private final String orderNumber;
    private final String status;
    private final LocalDateTime createdAt;

    public OrderGetResponse(Long id, String orderNumber, String status, LocalDateTime createdAt) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.status = status;
        this.createdAt = createdAt;
    }
}
