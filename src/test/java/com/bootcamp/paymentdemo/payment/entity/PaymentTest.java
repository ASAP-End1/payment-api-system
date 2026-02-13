package com.bootcamp.paymentdemo.payment.entity;

import com.bootcamp.paymentdemo.payment.consts.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTest {

    @Test
    @DisplayName("결제 생성 빌더 테스트")
    void builderTest() {
        Payment payment = Payment.builder()
                .dbPaymentId("imp_123")
                .totalAmount(BigDecimal.valueOf(10000))
                .status(PaymentStatus.PENDING)
                .build();

        assertThat(payment.getDbPaymentId()).isEqualTo("imp_123");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPointsToUse()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("completePayment: 결제 완료 상태 변경")
    void completePayment() {
        Payment payment = Payment.builder().status(PaymentStatus.PENDING).build();

        payment.completePayment("pg_key_123");

        assertThat(payment.getPaymentId()).isEqualTo("pg_key_123");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("isAlreadyProcessed: 이미 처리된 결제인지 확인")
    void isAlreadyProcessed() {
        Payment pending = Payment.builder().status(PaymentStatus.PENDING).build();
        Payment paid = Payment.builder().status(PaymentStatus.PAID).build();
        Payment refund = Payment.builder().status(PaymentStatus.REFUND).build();
        Payment fail = Payment.builder().status(PaymentStatus.FAIL).build();

        assertThat(pending.isAlreadyProcessed()).isFalse();
        assertThat(paid.isAlreadyProcessed()).isTrue();
        assertThat(refund.isAlreadyProcessed()).isTrue();
        assertThat(fail.isAlreadyProcessed()).isTrue();
    }

    @Test
    @DisplayName("cancelPointUsage: 사용 포인트 및 금액 초기화")
    void cancelPointUsage() {
        Payment payment = Payment.builder()
                .totalAmount(BigDecimal.valueOf(10000))
                .pointsToUse(BigDecimal.valueOf(1000))
                .build();

        payment.cancelPointUsage();

        assertThat(payment.getTotalAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(payment.getPointsToUse()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("환불 상태 및 로직 테스트")
    void refundTest() {
        Payment payment = Payment.builder().status(PaymentStatus.PAID).build();

        assertThat(payment.isPaid()).isTrue();

        payment.refund();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND);
        assertThat(payment.isRefund()).isTrue();
    }
}