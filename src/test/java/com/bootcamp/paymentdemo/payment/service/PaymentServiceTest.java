package com.bootcamp.paymentdemo.payment.service;

import com.bootcamp.paymentdemo.order.consts.OrderStatus;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.repository.OrderRepository;
import com.bootcamp.paymentdemo.payment.consts.PaymentStatus;
import com.bootcamp.paymentdemo.payment.dto.PaymentConfirmResponse;
import com.bootcamp.paymentdemo.payment.dto.PaymentCreateRequest;
import com.bootcamp.paymentdemo.payment.dto.PaymentCreateResponse;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        // 테스트용 주문 객체 생성 (ID 포함)
        testOrder = Order.builder()
                .orderNumber("ORD-20240101")
                .totalAmount(new BigDecimal("10000"))
                .orderStatus(OrderStatus.PENDING_CONFIRMATION)
                .build();

        // Reflection을 사용해 ID 강제 주입 (보통 DB가 생성해주지만 테스트를 위해 필요)
        ReflectionTestUtils.setField(testOrder, "id", 1L);
    }
    @Test
    @DisplayName("결제 생성 성공: 주문 조회 후 PENDING 상태의 결제 데이터를 저장한다")
    void createPayment_Success() {

        // 1. 요청 객체 생성 (orderId는 "1"로 설정)
        PaymentCreateRequest request = new PaymentCreateRequest("1", new BigDecimal("10000"), BigDecimal.ZERO);
        given(orderRepository.findByOrderNumber(anyString()))
                .willReturn(Optional.of(testOrder));

        // [When] 실제 결제 생성 로직 실행
        PaymentCreateResponse response = paymentService.createPayment(request);

        // [Then] 결과 검증
        assertAll(
                () -> assertThat(response.isSuccess()).isTrue(),
                () -> assertThat(response.getStatus()).isEqualTo("PENDING"),
                () -> assertThat(response.getPaymentId()).startsWith("order_mid_"),

                () -> verify(paymentRepository, times(1)).save(any(Payment.class))
        );
    }

    @Test
    @DisplayName("결제 확정 성공 - DB에서 결제 건을 찾아 상태를 PAID로 변경한다")
    void confirmPayment_Success() {
        // given
        String dbPaymentId = "order_mid_12345";
        String impUid = "imp_987654321";

        Payment pendingPayment = Payment.builder()
                .dbPaymentId(dbPaymentId)
                .order(testOrder)
                .totalAmount(new BigDecimal("10000"))
                .status(PaymentStatus.PENDING)
                .build();

        given(paymentRepository.findByDbPaymentId(dbPaymentId)).willReturn(Optional.of(pendingPayment));

        // when
        PaymentConfirmResponse response = paymentService.confirmPayment(dbPaymentId, impUid);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getStatus()).isEqualTo("PAID");
        assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(pendingPayment.getPaymentId()).isEqualTo(impUid);
    }

    @Test
    @DisplayName("결제 확정 실패 - 존재하지 않는 dbPaymentId인 경우 실패 응답을 반환한다")
    void confirmPayment_Fail_NotFound() {
        // given
        given(paymentRepository.findByDbPaymentId("wrong_id")).willReturn(Optional.empty());

        // when
        PaymentConfirmResponse response = paymentService.confirmPayment("wrong_id", "imp_uid");

        // then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getStatus()).isEqualTo("FAILED");
    }
}