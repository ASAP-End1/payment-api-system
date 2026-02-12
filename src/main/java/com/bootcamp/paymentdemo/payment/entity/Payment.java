package com.bootcamp.paymentdemo.payment.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.payment.consts.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "payments", schema = "test")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 우리 서버가 생성한 ID (결제 요청 시 사용)
    @Column(name = "db_payment_id", unique = true, nullable = false)
    private String dbPaymentId;

    // PortOne에서 발급받은 영수증 ID (결제 완료 후 기록)
    @Column(name = "payment_id", unique = true)
    private String paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "points_to_use")
    private BigDecimal pointsToUse;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Builder
    public Payment(String dbPaymentId, Order order, BigDecimal totalAmount, BigDecimal pointsToUse, PaymentStatus status) {
        this.dbPaymentId = dbPaymentId;
        this.order = order;
        this.totalAmount = totalAmount;
        this.pointsToUse = pointsToUse != null ? pointsToUse : BigDecimal.ZERO;
        this.status = status;
    }

    // 결제 완료 시 호출
    public void completePayment(String paymentId) {
        this.paymentId = paymentId;
        this.status = PaymentStatus.PAID;
    }

    public boolean isAlreadyProcessed() {

        return this.status == PaymentStatus.PAID ||
                this.status == PaymentStatus.REFUND ||
                this.status == PaymentStatus.FAIL;
    }

    // 환불 시 결제 상태 검증
    public boolean isCompleted() {
        return this.status == PaymentStatus.PAID;
    }

    // 환불 시 결제 상태 검증
    public boolean isRefund() {
        return this.status == PaymentStatus.REFUND;
    }

    // 환불 후 상태 변경
    public void refund() {
        this.status = PaymentStatus.REFUND;
    }
}

