package com.bootcamp.paymentdemo.payment.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import com.bootcamp.paymentdemo.payment.consts.PaymentStatus;
import jakarta.persistence.*;
import jakarta.persistence.GenerationType;
import lombok.AccessLevel;
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
    private Long id; // DB 내부 관리를 위한 PK

    // PortOneSDK에서 넘어오는 값
    @Column(name = "payment_id", unique = true)
    private String paymentId;

    // 주문 ID (연관관계 객체)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 실제 결제 요청 금액
    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    // 사용자가 사용 신청한 포인트
    @Column(name = "points_to_use")
    private BigDecimal pointsToUse;

    // 현재 결제 상태
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

}
