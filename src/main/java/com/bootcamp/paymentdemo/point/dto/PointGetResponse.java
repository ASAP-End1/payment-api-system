package com.bootcamp.paymentdemo.point.dto;

import com.bootcamp.paymentdemo.point.entity.PointType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PointGetResponse {

    private final Long id;
    private final Long orderId;
    private final int amount;
    private final PointType pointType;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;

    public PointGetResponse(Long id, Long orderId, int amount, PointType pointType, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.pointType = pointType;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
}
