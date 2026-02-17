package com.bootcamp.paymentdemo.point.dto;

import com.bootcamp.paymentdemo.point.consts.PointType;
import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class PointGetResponse {

    private final Long id;
    private final Long orderId;
    private final BigDecimal amount;
    private final PointType pointType;
    private final LocalDateTime createdAt;
    private final LocalDate expiresAt;

    public PointGetResponse(Long id, Long orderId, BigDecimal amount, PointType pointType, LocalDateTime createdAt, LocalDate expiresAt) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.pointType = pointType;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    // 정적 팩토리 메서드
    public static PointGetResponse from(PointTransaction pointTransaction) {
        return new PointGetResponse(
                pointTransaction.getId(),
                // EXPIRED 타입은 Order가 null
                pointTransaction.getOrder() != null ? pointTransaction.getOrder().getId() : null,
                pointTransaction.getAmount(),
                pointTransaction.getType(),
                pointTransaction.getCreatedAt(),
                pointTransaction.getExpiresAt()
        );
    }
}
