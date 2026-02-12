package com.bootcamp.paymentdemo.payment.service;

import com.bootcamp.paymentdemo.external.portone.client.PortOneClient;
import com.bootcamp.paymentdemo.external.portone.dto.PortOneCancelRequest;
import com.bootcamp.paymentdemo.external.portone.dto.PortOnePaymentResponse;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.repository.OrderRepository;
import com.bootcamp.paymentdemo.order.service.OrderService;
import com.bootcamp.paymentdemo.payment.consts.PaymentStatus;
import com.bootcamp.paymentdemo.payment.dto.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PortOneClient portOneClient;
    @Mock
    private OrderService orderService;

    @InjectMocks
    private PaymentService paymentService;

    private Order testOrder;
    private final String dbPaymentId = "order_mid_12345";
    private final String impUid = "imp_987654321";

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .orderNumber("ORD-20240101")
                .totalAmount(new BigDecimal("10000"))
                .usedPoints(new BigDecimal("1000"))
                .finalAmount(new BigDecimal("9000"))
                .orderStatus(com.bootcamp.paymentdemo.order.consts.OrderStatus.PENDING_PAYMENT) // 이 부분!
                .build();

        ReflectionTestUtils.setField(testOrder, "id", 1L);
    }

    @Test
    @DisplayName("결제 생성 성공")
    void createPayment_Success() {
        // given
        PaymentCreateRequest request = new PaymentCreateRequest("ORD-20240101", new BigDecimal("10000"), new BigDecimal("1000"));
        given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(testOrder));

        // when
        PaymentCreateResponse response = paymentService.createPayment(request);

        // then
        assertAll(
                () -> assertThat(response.isSuccess()).isTrue(),
                () -> assertThat(response.getStatus()).isEqualTo("PENDING"),
                () -> verify(paymentRepository).save(any(Payment.class))
        );
    }

    @Test
    @DisplayName("결제 확정 성공 -> 오더서비스로 전달 예정")
    void confirmPayment_Success() {
        // given
        Payment pendingPayment = Payment.builder()
                .dbPaymentId(dbPaymentId)
                .order(testOrder)
                .status(PaymentStatus.PENDING)
                .build();

        given(paymentRepository.findByDbPaymentId(dbPaymentId)).willReturn(Optional.of(pendingPayment));

        PortOnePaymentResponse mockResponse = mock(PortOnePaymentResponse.class);
        PortOnePaymentResponse.Amount mockAmount = mock(PortOnePaymentResponse.Amount.class);

        given(mockAmount.total()).willReturn(new BigDecimal("9000"));
        given(mockResponse.amount()).willReturn(mockAmount);
        given(portOneClient.getPayment(impUid)).willReturn(mockResponse);

        // when
        PaymentConfirmResponse response = paymentService.confirmPayment(dbPaymentId, impUid);

        // then
        assertAll(
                () -> assertThat(response.isSuccess()).isTrue(),
                () -> assertThat(response.getStatus()).isEqualTo("PAID"),
                () -> verify(orderService).completePayment(testOrder.getId()),
                () -> verify(orderService).confirmOrder(testOrder.getId()),
                () -> assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.PAID)
        );
    }

    @Test
    @DisplayName("결제 확정 실패: 포트원에서 준 금액이랑 우리 금액이랑 달라서 실패")
    void confirmPayment_Fail_AmountMismatch() {
        // given
        Payment pendingPayment = Payment.builder()
                .dbPaymentId(dbPaymentId)
                .order(testOrder)
                .status(PaymentStatus.PENDING)
                .build();

        given(paymentRepository.findByDbPaymentId(dbPaymentId)).willReturn(Optional.of(pendingPayment));

        PortOnePaymentResponse mockResponse = mock(PortOnePaymentResponse.class);
        PortOnePaymentResponse.Amount mockAmount = mock(PortOnePaymentResponse.Amount.class);

        given(mockAmount.total()).willReturn(new BigDecimal("5000"));
        given(mockResponse.amount()).willReturn(mockAmount);
        given(portOneClient.getPayment(impUid)).willReturn(mockResponse);

        // when
        PaymentConfirmResponse response = paymentService.confirmPayment(dbPaymentId, impUid);

        // then
        assertAll(
                () -> assertThat(response.isSuccess()).isFalse(),
                () -> assertThat(response.getStatus()).isEqualTo("AMOUNT_MISMATCH"),
                () -> verify(orderService).cancelOrder(eq(testOrder.getId()), contains("위변조")),
                () -> verify(portOneClient).cancelPayment(eq(impUid), any(PortOneCancelRequest.class))
        );
    }

    @Test
    @DisplayName("결제 확정 실패: 내부로직 문제")
    void confirmPayment_Fail_InternalError() {
        // given
        Payment pendingPayment = Payment.builder()
                .dbPaymentId(dbPaymentId)
                .order(testOrder)
                .status(PaymentStatus.PENDING)
                .build();

        given(paymentRepository.findByDbPaymentId(dbPaymentId)).willReturn(Optional.of(pendingPayment));

        PortOnePaymentResponse mockResponse = mock(PortOnePaymentResponse.class);
        PortOnePaymentResponse.Amount mockAmount = mock(PortOnePaymentResponse.Amount.class);
        given(mockAmount.total()).willReturn(new BigDecimal("9000"));
        given(mockResponse.amount()).willReturn(mockAmount);
        given(portOneClient.getPayment(impUid)).willReturn(mockResponse);


        doThrow(new RuntimeException("DB 장애")).when(orderService).completePayment(anyLong());

        // when
        PaymentConfirmResponse response = paymentService.confirmPayment(dbPaymentId, impUid);

        // then
        assertAll(
                () -> assertThat(response.isSuccess()).isFalse(),
                () -> assertThat(response.getStatus()).isEqualTo("FAILED"),
                () -> verify(orderService).cancelOrder(eq(testOrder.getId()), contains("내부 오류")),
                () -> verify(portOneClient).cancelPayment(eq(impUid), any(PortOneCancelRequest.class))
        );
    }
}