package com.bootcamp.paymentdemo.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 사용자별 총 결제 금액 집계 테이블
@Entity
@Table(name = "user_paid_amounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPaidAmount {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "total_paid_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalPaidAmount;  // 사용자의 총 결제 금액

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 사용자 회원가입시 0원 설정
    public static UserPaidAmount createDefault(User user) {
        UserPaidAmount paidAmount = new UserPaidAmount();

        paidAmount.user = user;
        paidAmount.totalPaidAmount = BigDecimal.ZERO;
        paidAmount.updatedAt = LocalDateTime.now();

        return paidAmount;
    }

    // payments.status = 'PAID' (결제 완료) 시
    // 결제 금액 합산 (결제 요청 금액 -> totalAmount)
    public void addPaidAmount(BigDecimal paymentAmount) {
        if(paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다.");
        }
        this.totalPaidAmount = this.totalPaidAmount.add(paymentAmount);
        this.updatedAt = LocalDateTime.now();
    }

    // 환불 시
    // 결제 금액 감소 (환불 요청 금액 -> refundAmount)
    public void subtractPaidAmount(BigDecimal refundAmount) {
        if(refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다.");
        }
        BigDecimal newAmount = this.totalPaidAmount.subtract(refundAmount);

        // 총 결제 금액이 음수가 되지 않도록함
        this.totalPaidAmount = newAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newAmount;
        this.updatedAt = LocalDateTime.now();
    }


}
