package com.bootcamp.paymentdemo.order.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class OrderProductGetResponse {

    private final Long productId;
    private final String productName;
    private final BigDecimal orderPrice;  // 주문 당시 가격
    private final int count;              // 수량
    private final BigDecimal subtotal;    // 소계 (가격 × 수량)

    public OrderProductGetResponse(Long productId, String productName,
                                   BigDecimal orderPrice, int count) {
        this.productId = productId;
        this.productName = productName;
        this.orderPrice = orderPrice;
        this.count = count;
        this.subtotal = orderPrice.multiply(BigDecimal.valueOf(count));
    }
}
