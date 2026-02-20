package com.bootcamp.paymentdemo.order.consts;

import lombok.Getter;

@Getter
public enum OrderStatus {
    PENDING_PAYMENT("PENDING_PAYMENT"),
    PENDING_CONFIRMATION("PENDING_CONFIRMATION"),
    CONFIRMED("CONFIRMED"),
    CANCELLED("CANCELLED");

    private final String OrderStatusName;
    OrderStatus(String OrderStatusName) {this.OrderStatusName = OrderStatusName;}

    public boolean canBeCancelled() {
       return this != CONFIRMED && this != CANCELLED;
    }
}
