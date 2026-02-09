package com.bootcamp.paymentdemo.domain.order.consts;

import lombok.Getter;

@Getter
public enum OrderStatus {
    PENDING_PAYMENT("PENDING_PAYMENT"), // 결제 대기
    PENDING_CONFIRMATION("PENDING_CONFIRMATION"), // 주문 확정 대기
    CONFIRMED("CONFIRMED"), // 주문 확정
    CANCELLED("CANCELLED"); // 주문 취소

    private final String OrderStatusName;
    OrderStatus(String OrderStatusName) {this.OrderStatusName = OrderStatusName;}

    public boolean canBeCancelled() {
       return this != CONFIRMED && this != CANCELLED;
    }
}
