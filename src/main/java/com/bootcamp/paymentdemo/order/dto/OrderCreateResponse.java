package com.bootcamp.paymentdemo.order.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class OrderCreateResponse {

    private final Long id;
    private final String orderNumber;
    private final BigDecimal totalAmount;      // 포인트 차감 전 총 금액
    private final BigDecimal usedPoints;       // 사용된 포인트
    private final BigDecimal finalAmount;      // 포인트 차감 후 최종 결제 금액
    private final BigDecimal earnedPoints;     // 적립될 포인트
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
}
