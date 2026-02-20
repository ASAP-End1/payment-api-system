package com.bootcamp.paymentdemo.payment.service;

import com.bootcamp.paymentdemo.external.portone.client.PortOneClient;
import com.bootcamp.paymentdemo.external.portone.dto.PortOneCancelRequest;
import com.bootcamp.paymentdemo.external.portone.dto.PortOnePaymentResponse;
import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.repository.OrderRepository;
import com.bootcamp.paymentdemo.order.service.OrderService;
import com.bootcamp.paymentdemo.payment.consts.PaymentStatus;
import com.bootcamp.paymentdemo.payment.dto.*;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.repository.PaymentRepository;
import com.bootcamp.paymentdemo.user.entity.User;
import com.bootcamp.paymentdemo.user.entity.UserPointBalance;
import com.bootcamp.paymentdemo.user.repository.UserPointBalanceRepository;
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
    @Mock
    private UserPointBalanceRepository userPointBalanceRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Order testOrder;
    private User testUser;
    private final String dbPaymentId = "order_mid_12345";

    @BeforeEach
    void setUp() {
        Membership dummyGrade = mock(Membership.class);

        testUser = User.register(
                "test@example.com",
                "pw",
                "테스트유저",
                "010-1234-5678",
                dummyGrade
        );
        ReflectionTestUtils.setField(testUser, "userId", 1L);

        testOrder = Order.builder()
                .orderNumber("ORD-20240101")
                .user(testUser)
                .totalAmount(new BigDecimal("10000"))
                .usedPoints(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("10000"))
                .orderStatus(com.bootcamp.paymentdemo.order.consts.OrderStatus.PENDING_PAYMENT)
                .build();

        ReflectionTestUtils.setField(testOrder, "id", 100L);
    }

    @Test
    @DisplayName("결제 생성 성공: 포인트 사용 안함")
    void createPayment_Success_NoPoints() {
        PaymentCreateRequest request = new PaymentCreateRequest();
        ReflectionTestUtils.setField(request, "orderNumber", "ORD-20240101");
        ReflectionTestUtils.setField(request, "totalAmount", new BigDecimal("10000"));
        ReflectionTestUtils.setField(request, "pointsToUse", null);

        given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(testOrder));

        PaymentCreateResponse response = paymentService.createPayment(request);

        assertAll(
                () -> assertThat(response.isSuccess()).isTrue(),
                () -> assertThat(response.getStatus()).isEqualTo("PENDING"),
                () -> assertThat(testOrder.getUsedPoints()).isEqualByComparingTo(BigDecimal.ZERO),
                () -> verify(paymentRepository).save(any(Payment.class))
        );
    }

    @Test
    @DisplayName("결제 생성 성공: 포인트 사용")
    void createPayment_Success_WithPoints() {
        BigDecimal pointsToUse = new BigDecimal("1000");
        PaymentCreateRequest request = new PaymentCreateRequest();
        ReflectionTestUtils.setField(request, "orderNumber", "ORD-20240101");
        ReflectionTestUtils.setField(request, "totalAmount", new BigDecimal("10000"));
        ReflectionTestUtils.setField(request, "pointsToUse", pointsToUse);

        given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(testOrder));

        UserPointBalance mockBalance = UserPointBalance.builder()
                .userId(testUser.getUserId())
                .currentPoints(new BigDecimal("2000"))
                .build();
        given(userPointBalanceRepository.findById(testUser.getUserId())).willReturn(Optional.of(mockBalance));

        PaymentCreateResponse response = paymentService.createPayment(request);

        assertAll(
                () -> assertThat(response.isSuccess()).isTrue(),
                () -> assertThat(testOrder.getUsedPoints()).isEqualByComparingTo(pointsToUse),
                () -> verify(paymentRepository).save(any(Payment.class))
        );
    }

    @Test
    @DisplayName("결제 생성 실패: 포인트 잔액 부족")
    void createPayment_Fail_NotEnoughPoints() {
        BigDecimal pointsToUse = new BigDecimal("5000");
        PaymentCreateRequest request = new PaymentCreateRequest();
        ReflectionTestUtils.setField(request, "orderNumber", "ORD-20240101");
        ReflectionTestUtils.setField(request, "totalAmount", new BigDecimal("10000"));
        ReflectionTestUtils.setField(request, "pointsToUse", pointsToUse);

        given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(testOrder));

        UserPointBalance mockBalance = UserPointBalance.builder()
                .userId(testUser.getUserId())
                .currentPoints(new BigDecimal("1000"))
                .build();
        given(userPointBalanceRepository.findById(testUser.getUserId())).willReturn(Optional.of(mockBalance));

        assertThrows(IllegalArgumentException.class, () -> paymentService.createPayment(request));
    }

    @Test
    @DisplayName("결제 확정 성공")
    void confirmPayment_Success() {
        Payment pendingPayment = Payment.builder()
                .dbPaymentId(dbPaymentId)
                .order(testOrder)
                .totalAmount(testOrder.getTotalAmount())
                .pointsToUse(BigDecimal.ZERO)
                .status(PaymentStatus.PENDING)
                .build();

        given(paymentRepository.findByDbPaymentIdWithLock(dbPaymentId)).willReturn(Optional.of(pendingPayment));

        PortOnePaymentResponse mockResponse = mock(PortOnePaymentResponse.class);
        PortOnePaymentResponse.Amount mockAmount = mock(PortOnePaymentResponse.Amount.class);

        given(mockAmount.total()).willReturn(testOrder.getFinalAmount());
        given(mockResponse.amount()).willReturn(mockAmount);
        given(portOneClient.getPayment(dbPaymentId)).willReturn(mockResponse);

        PaymentConfirmResponse response = paymentService.confirmPayment(dbPaymentId);

        assertAll(
                () -> assertThat(response.getOrderId()).isEqualTo(testOrder.getId()),
                () -> verify(orderService).completePayment(testOrder.getId()),
                () -> assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.PAID)
        );
    }

    @Test
    @DisplayName("결제 확정 실패: 금액 불일치 (자동 취소 수행)")
    void confirmPayment_Fail_AmountMismatch() {
        Payment pendingPayment = Payment.builder()
                .dbPaymentId(dbPaymentId)
                .order(testOrder)
                .totalAmount(testOrder.getTotalAmount())
                .pointsToUse(new BigDecimal("1000"))
                .status(PaymentStatus.PENDING)
                .build();

        Payment spyPayment = spy(pendingPayment);

        given(paymentRepository.findByDbPaymentIdWithLock(dbPaymentId)).willReturn(Optional.of(spyPayment));

        PortOnePaymentResponse mockResponse = mock(PortOnePaymentResponse.class);
        PortOnePaymentResponse.Amount mockAmount = mock(PortOnePaymentResponse.Amount.class);

        given(mockAmount.total()).willReturn(new BigDecimal("5000"));
        given(mockResponse.amount()).willReturn(mockAmount);
        given(portOneClient.getPayment(dbPaymentId)).willReturn(mockResponse);

        PaymentConfirmResponse response = paymentService.confirmPayment(dbPaymentId);

        assertAll(
                () -> assertThat(response.getOrderId()).isNull(),
                () -> verify(orderService).rollbackUsedPoint(testOrder.getId()),
                () -> verify(spyPayment).cancelPointUsage(),
                () -> verify(portOneClient).cancelPayment(eq(dbPaymentId), any(PortOneCancelRequest.class))
        );
    }

    @Test
    @DisplayName("결제 확정 실패: 내부 로직 에러 (보상 트랜잭션 수행)")
    void confirmPayment_Fail_InternalError() {
        Payment pendingPayment = Payment.builder()
                .dbPaymentId(dbPaymentId)
                .order(testOrder)
                .totalAmount(testOrder.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .build();

        Payment spyPayment = spy(pendingPayment);

        given(paymentRepository.findByDbPaymentIdWithLock(dbPaymentId)).willReturn(Optional.of(spyPayment));

        PortOnePaymentResponse mockResponse = mock(PortOnePaymentResponse.class);
        PortOnePaymentResponse.Amount mockAmount = mock(PortOnePaymentResponse.Amount.class);
        given(mockAmount.total()).willReturn(testOrder.getFinalAmount());
        given(mockResponse.amount()).willReturn(mockAmount);
        given(portOneClient.getPayment(dbPaymentId)).willReturn(mockResponse);

        doThrow(new RuntimeException("DB Error")).when(orderService).completePayment(anyLong());

        PaymentConfirmResponse response = paymentService.confirmPayment(dbPaymentId);

        assertAll(
                () -> assertThat(response.getOrderId()).isNull(),
                () -> verify(orderService).rollbackUsedPoint(testOrder.getId()),
                () -> verify(spyPayment).cancelPointUsage(),
                () -> verify(portOneClient).cancelPayment(eq(dbPaymentId), any(PortOneCancelRequest.class))
        );
    }


}