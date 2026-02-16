package com.bootcamp.paymentdemo.order.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import com.bootcamp.paymentdemo.order.consts.OrderStatus;
import com.bootcamp.paymentdemo.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_number", unique = true, nullable = false, length = 50)
    private String orderNumber;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount; // 포인트 차감 전

    @Column(name = "used_points", nullable = false, precision = 18, scale = 2)
    private BigDecimal usedPoints; // 사용한 포인트

    @Column(name = "final_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal finalAmount; // 포인트 차감 후

    // [추가 요청] 적립 예정 포인트 기록 (선택 사항이지만 데이터 정합성을 위해 권장)
    @Setter
    @Column(name = "earned_points", nullable = false, precision = 18, scale = 2)
    private BigDecimal earnedPoints;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "order_status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Builder
    public Order(User user, String orderNumber, BigDecimal totalAmount, BigDecimal usedPoints, BigDecimal finalAmount, BigDecimal earnedPoints, String currency, OrderStatus orderStatus) {
        this.user = user;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.usedPoints = usedPoints;
        this.finalAmount = finalAmount;
        this.earnedPoints = earnedPoints;
        this.currency = currency;
        this.orderStatus = orderStatus;
    }

    // 결제 완료 처리 (PENDING_PAYMENT → PENDING_CONFIRMATION)
    public void completePayment() {
        if (this.orderStatus != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("결제 대기 상태의 주문만 결제 완료 처리할 수 있습니다.");
        }
        this.orderStatus = OrderStatus.PENDING_CONFIRMATION;
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

    // confirmPayment 시 상태 변경
    public void pendingConfirmation() {
        if (this.orderStatus != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("결제 대기 상태의 주문만 확정 대기할 수 있습니다.");
        }
        this.orderStatus = OrderStatus.PENDING_CONFIRMATION;
    }

    // 환불 시 주문 상태 검증
    public boolean isAwaitingConfirmation() {
        return this.orderStatus == OrderStatus.PENDING_CONFIRMATION;
    }

    // 환불 시 주문 상태 검증
    public boolean isCanceled() {
        return this.orderStatus == OrderStatus.CANCELLED;
    }

    // 포인트를 사용하기 위해서 구현한 로직 <- 윤민기
    public void applyPointDiscount(BigDecimal usagePoints) {
        if (usagePoints == null) {
            usagePoints = BigDecimal.ZERO;
        }

        if (usagePoints.compareTo(this.totalAmount) > 0) {
            throw new IllegalArgumentException("포인트 사용액은 결제 총액을 초과할 수 없습니다.");
        }

        this.usedPoints = usagePoints;
        this.finalAmount = this.totalAmount.subtract(usagePoints);
    }

    public void cancelPointUsage() {
        this.usedPoints = BigDecimal.ZERO;
        this.finalAmount = this.totalAmount;
    }

}
