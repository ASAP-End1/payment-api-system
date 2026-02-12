package com.bootcamp.paymentdemo.refund.entity;

import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.refund.consts.RefundStatus;
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

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "refund_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefundStatus status;

    // PortOne에서 반환하는 환불 ID
    @Column(name = "portone_refund_id")
    private String portOneRefundId;

    @Column(name = "refund_group_id", nullable = false)
    private String refundGroupId;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;


    @Builder(access = AccessLevel.PRIVATE)
    private Refund(Long paymentId, BigDecimal refundAmount, String reason, RefundStatus status, String portOneRefundId,  String refundGroupId) {
        this.paymentId = paymentId;
        this.refundAmount = refundAmount;
        this.reason = reason;
        this.status = status;
        this.portOneRefundId = portOneRefundId;
        this.refundGroupId = refundGroupId;
        this.refundedAt = LocalDateTime.now();
    }

    // 환불 요청 이력 생성
    public static Refund createRequest(Long paymentId, BigDecimal refundAmount, String reason, String refundGroupId) {
        return Refund.builder()
                .paymentId(paymentId)
                .refundAmount(refundAmount)
                .reason(reason)
                .portOneRefundId(null)
                .refundGroupId(refundGroupId)
                .status(RefundStatus.PENDING)
                .build();
    }

    // 환불 완료 이력 생성
    public static Refund createCompleted(Long paymentId, BigDecimal refundAmount, String reason, String portOneRefundId, String refundGroupId) {
        return Refund.builder()
                .paymentId(paymentId)
                .refundAmount(refundAmount)
                .reason(reason)
                .portOneRefundId(portOneRefundId)
                .refundGroupId(refundGroupId)
                .status(RefundStatus.COMPLETED)
                .build();
    }


    // 환불 실패 이력 생성
    public static Refund createFailed(Long paymentId, BigDecimal refundAmount, String reason, String portOneRefundId, String refundGroupId) {
        return Refund.builder()
                .paymentId(paymentId)
                .refundAmount(refundAmount)
                .reason(reason)
                .portOneRefundId(portOneRefundId)
                .refundGroupId(refundGroupId)
                .status(RefundStatus.FAILED)
                .build();
    }

}
