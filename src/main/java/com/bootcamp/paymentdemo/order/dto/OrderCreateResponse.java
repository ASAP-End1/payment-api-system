package com.bootcamp.paymentdemo.order.dto;

import com.bootcamp.paymentdemo.order.entity.Order;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class OrderCreateResponse {

    private final Long id;
    private final String orderNumber;
    private final BigDecimal totalAmount;
    private final BigDecimal usedPoints;
    private final BigDecimal finalAmount;
    private final BigDecimal earnedPoints;
    private final String status;


    public OrderCreateResponse(Long id, String orderNumber, BigDecimal totalAmount,
                                BigDecimal usedPoints, BigDecimal finalAmount,
                                BigDecimal earnedPoints, String status) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.usedPoints = usedPoints;
        this.finalAmount = finalAmount;
        this.earnedPoints = earnedPoints;
        this.status = status;
    }


    public static OrderCreateResponse from(Order order) {
        return new OrderCreateResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getUsedPoints(),
                order.getFinalAmount(),
                order.getEarnedPoints(),
                order.getOrderStatus().name()
        );
    }
}
