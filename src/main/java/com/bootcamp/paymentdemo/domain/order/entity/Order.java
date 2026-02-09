package com.bootcamp.paymentdemo.domain.order.entity;

import com.bootcamp.paymentdemo.domain.order.consts.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId; // 사용자 테이블 완성되면 수정
    private String orderNumber;
    private BigDecimal totalAmount; // 포인트 차감 전
    private BigDecimal usedPoints; // 포인트 테이블이 완성되면 수정
    private BigDecimal finalAmount; // 포인트 차감 후
    private BigDecimal earnedPoints;
    private String currency;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Builder
    public Order(String userId, String orderNumber, BigDecimal totalAmount, BigDecimal usedPoints, BigDecimal finalAmount, BigDecimal earnedPoints, String currency, OrderStatus orderStatus) {
        this.userId = userId;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.usedPoints = usedPoints;
        this.finalAmount = finalAmount;
        this.earnedPoints = earnedPoints;
        this.currency = currency;
        this.orderStatus = orderStatus;
    }

    // 주문 취소 (확정된 주문과 취소된 주문은 취소 할 수 없음)
    public void cancel() {
        if (!this.orderStatus.canBeCancelled()) {
            if (this.orderStatus == OrderStatus.CONFIRMED) {
                throw new IllegalStateException("이미 확정된 주문은 취소할 수 없습니다.");
            }
            if (this.orderStatus == OrderStatus.CANCELLED) {
                throw new IllegalStateException("이미 취소된 주문입니다.");
            }
        }
        this.orderStatus = OrderStatus.CANCELLED;
    }

    // 주문 확정
    public void confirm() {
        if (this.orderStatus != OrderStatus.PENDING_CONFIRMATION) {
            throw new IllegalStateException("주문 확정 대기 상태의 주문만 확정할 수 있습니다.");
        }
        this.orderStatus = OrderStatus.CONFIRMED;
    }

    // 환불 시 주문 상태 검증
    public boolean isAwaitingConfirmation() {
        return this.orderStatus == OrderStatus.PENDING_CONFIRMATION;
    }
}
