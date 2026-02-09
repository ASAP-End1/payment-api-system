package com.bootcamp.paymentdemo.refund.entity;

import com.bootcamp.paymentdemo.payment.entity.Payment;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long refundId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "refund_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefundStatus status;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Refund(Payment payment, BigDecimal refundAmount, String reason, RefundStatus status) {
        this.payment = payment;
        this.refundAmount = refundAmount;
        this.reason = reason;
        this.status = status;
        this.refundedAt = LocalDateTime.now();
    }

    // 환불 요청 이력 생성
    public static Refund createRequest(Payment payment, BigDecimal refundAmount, String reason) {
        return Refund.builder()
                .payment(payment)
                .refundAmount(refundAmount)
                .reason(reason)
                .status(RefundStatus.PENDING)
                .build();
    }

    // 환불 완료 이력 생성
    public static Refund createCompleted(Payment payment, BigDecimal refundAmount, String reason) {
        return Refund.builder()
                .payment(payment)
                .refundAmount(refundAmount)
                .reason(reason)
                .status(RefundStatus.COMPLETED)
                .build();
    }


    // 환불 실패 이력 생성
    public static Refund createFailed(Payment payment, BigDecimal refundAmount, String reason) {
        return Refund.builder()
                .payment(payment)
                .refundAmount(refundAmount)
                .reason(reason)
                .status(RefundStatus.FAILED)
                .build();
    }

}
