package com.bootcamp.paymentdemo.refund.service;

import com.bootcamp.paymentdemo.external.portone.client.PortOneClient;
import com.bootcamp.paymentdemo.external.portone.dto.PortOneRefundRequest;
import com.bootcamp.paymentdemo.external.portone.dto.PortOneRefundResponse;
import com.bootcamp.paymentdemo.external.portone.exception.PortOneException;
import com.bootcamp.paymentdemo.membership.service.MembershipService;
import com.bootcamp.paymentdemo.order.consts.OrderStatus;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.service.OrderService;
import com.bootcamp.paymentdemo.orderProduct.entity.OrderProduct;
import com.bootcamp.paymentdemo.orderProduct.repository.OrderProductRepository;
import com.bootcamp.paymentdemo.payment.consts.PaymentStatus;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.repository.PaymentRepository;
import com.bootcamp.paymentdemo.product.service.ProductService;
import com.bootcamp.paymentdemo.refund.dto.RefundRequest;
import com.bootcamp.paymentdemo.refund.dto.RefundResponse;
import com.bootcamp.paymentdemo.refund.entity.Refund;
import com.bootcamp.paymentdemo.refund.exception.RefundException;
import com.bootcamp.paymentdemo.refund.repository.RefundRepository;
import com.bootcamp.paymentdemo.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private RefundRepository refundRepository;
    @Mock
    private RefundHistoryService refundHistoryService;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PortOneClient portOneClient;
    @Mock
    private OrderService orderService;
    @Mock
    private ProductService productService;
    @Mock
    private OrderProductRepository orderProductRepository;
    @Mock
    private MembershipService membershipService;

    @InjectMocks
    private RefundService refundService;

    private Payment testPayment;
    private Order testOrder;
    private User testUser;
    private String refundReason;
    private String portOneRefundId;

    @BeforeEach
    void setUp() {
        refundReason = "단순 변심";
        portOneRefundId = "portone-refund-456";

        // User 설정
        testUser = mock(User.class);

        // Order 설정
        testOrder = Order.builder()
                .user(testUser)
                .orderNumber("ORD-20240101")
                .totalAmount(new BigDecimal("10000"))
                .usedPoints(new BigDecimal("1000"))
                .finalAmount(new BigDecimal("9000"))
                .earnedPoints(BigDecimal.ZERO)
                .currency("KRW")
                .orderStatus(OrderStatus.PENDING_CONFIRMATION)
                .build();
        ReflectionTestUtils.setField(testOrder, "id", 100L);

        // Payment 설정
        testPayment = Payment.builder()
                .dbPaymentId("db1")
                .order(testOrder)
                .totalAmount(new BigDecimal("10000"))
                .pointsToUse(new BigDecimal("1000"))
                .status(PaymentStatus.PAID)
                .build();
        ReflectionTestUtils.setField(testPayment, "id", 1L);
        ReflectionTestUtils.setField(testPayment, "paymentId", "portone-payment-123");
    }

    @Test
    @DisplayName("정상적인 환불 요청 성공")
    void refundAll_Success() {
        // given
        given(testUser.getUserId()).willReturn(1L);

        RefundRequest refundRequest = createRefundRequest(refundReason);

        given(paymentRepository.findByDbPaymentIdWithLock(testPayment.getDbPaymentId()))
                .willReturn(Optional.of(testPayment));

        PortOneRefundResponse portOneResponse = createSuccessfulPortOneResponse();
        given(portOneClient.refundPayment(eq(testPayment.getPaymentId()), any(PortOneRefundRequest.class)))
                .willReturn(portOneResponse);

        List<OrderProduct> orderProducts = createOrderProducts(200L, 2);
        given(orderProductRepository.findByOrder_Id(testOrder.getId()))
                .willReturn(orderProducts);

        ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);

        // when
        RefundResponse response = refundService.refundAll(testPayment.getDbPaymentId(), refundRequest);

        // then
        assertAll(
                () -> assertThat(response).isNotNull(),
                () -> assertThat(response.getOrderId()).isEqualTo(testOrder.getId()),
                () -> assertThat(response.getOrderNumber()).isEqualTo(testOrder.getOrderNumber())
        );

        verify(refundHistoryService, times(1))
                .saveRequestHistory(
                        eq(testPayment.getId()),
                        eq(testPayment.getTotalAmount()),
                        eq(refundReason),
                        anyString()
                );

        ArgumentCaptor<PortOneRefundRequest> portOneRequestCaptor =
                ArgumentCaptor.forClass(PortOneRefundRequest.class);
        verify(portOneClient, times(1))
                .refundPayment(eq(testPayment.getPaymentId()), portOneRequestCaptor.capture());
        assertThat(portOneRequestCaptor.getValue().getReason()).isEqualTo(refundReason);

        verify(refundRepository, times(1)).save(refundCaptor.capture());
        verify(orderService, times(1)).cancelOrder(testOrder.getId(), refundReason);
        verify(productService, times(1)).increaseStock(200L, 2);
        verify(membershipService, times(1))
                .handleRefund(1L, testOrder.getFinalAmount(), testOrder.getId());

        assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.REFUND);
    }

    @Test
    @DisplayName("존재하지 않는 결제 ID로 환불 시도 시 예외 발생")
    void refundAll_PaymentNotFound() {
        // given
        RefundRequest refundRequest = createRefundRequest(refundReason);
        given(paymentRepository.findByDbPaymentIdWithLock(testPayment.getDbPaymentId()))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> refundService.refundAll(testPayment.getDbPaymentId(), refundRequest))
                .isInstanceOf(RefundException.class);

        verify(refundHistoryService, never())
                .saveRequestHistory(
                        eq(testPayment.getId()),
                        eq(testPayment.getTotalAmount()),
                        eq(refundReason),
                        anyString()
                );
        verify(portOneClient, never()).refundPayment(anyString(), any());
    }

    @Test
    @DisplayName("이미 환불된 결제에 대해 환불 시도 시 예외 발생")
    void refundAll_AlreadyRefunded() {
        // given
        RefundRequest refundRequest = createRefundRequest(refundReason);

        Payment refundedPayment = Payment.builder()
                .dbPaymentId("db1")
                .order(testOrder)
                .totalAmount(testPayment.getTotalAmount())
                .status(PaymentStatus.REFUND)
                .build();

        given(paymentRepository.findByDbPaymentIdWithLock(refundedPayment.getDbPaymentId()))
                .willReturn(Optional.of(refundedPayment));

        // when & then
        assertThatThrownBy(() -> refundService.refundAll(refundedPayment.getDbPaymentId(), refundRequest))
                .isInstanceOf(RefundException.class);

        verify(portOneClient, never()).refundPayment(anyString(), any());
    }

    @Test
    @DisplayName("이미 취소된 주문에 대해 환불 시도 시 예외 발생")
    void refundAll_AlreadyCanceled() {
        // given
        RefundRequest refundRequest = createRefundRequest(refundReason);

        Order cancelledOrder = Order.builder()
                .user(testUser)
                .orderNumber(testOrder.getOrderNumber())
                .totalAmount(testOrder.getTotalAmount())
                .usedPoints(testOrder.getUsedPoints())
                .finalAmount(testOrder.getFinalAmount())
                .earnedPoints(BigDecimal.ZERO)
                .currency("KRW")
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        Payment paymentWithCancelledOrder = Payment.builder()
                .dbPaymentId(testPayment.getDbPaymentId())
                .order(cancelledOrder)
                .totalAmount(testPayment.getTotalAmount())
                .status(PaymentStatus.PAID)
                .build();

        given(paymentRepository.findByDbPaymentIdWithLock(paymentWithCancelledOrder.getDbPaymentId()))
                .willReturn(Optional.of(paymentWithCancelledOrder));

        // when & then
        assertThatThrownBy(() -> refundService.refundAll(paymentWithCancelledOrder.getDbPaymentId(), refundRequest))
                .isInstanceOf(RefundException.class);

        verify(portOneClient, never()).refundPayment(anyString(), any());
    }

    @Test
    @DisplayName("결제되지 않은 상태에서 환불 시도 시 예외 발생")
    void refundAll_NotPaidStatus() {
        // given
        RefundRequest refundRequest = createRefundRequest(refundReason);

        Payment unpaidPayment = Payment.builder()
                .dbPaymentId(testPayment.getDbPaymentId())
                .order(testOrder)
                .totalAmount(testPayment.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .build();

        given(paymentRepository.findByDbPaymentIdWithLock(unpaidPayment.getDbPaymentId()))
                .willReturn(Optional.of(unpaidPayment));

        // when & then
        assertThatThrownBy(() -> refundService.refundAll(unpaidPayment.getDbPaymentId(), refundRequest))
                .isInstanceOf(RefundException.class);

        verify(portOneClient, never()).refundPayment(anyString(), any());
    }

    @Test
    @DisplayName("주문 확인 대기 상태가 아닐 때 환불 시도 시 예외 발생")
    void refundAll_InvalidOrderStatus() {
        // given
        RefundRequest refundRequest = createRefundRequest(refundReason);

        Order confirmedOrder = Order.builder()
                .user(testUser)
                .orderNumber(testOrder.getOrderNumber())
                .totalAmount(testOrder.getTotalAmount())
                .usedPoints(testOrder.getUsedPoints())
                .finalAmount(testOrder.getFinalAmount())
                .earnedPoints(BigDecimal.ZERO)
                .currency("KRW")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();

        Payment paymentWithConfirmedOrder = Payment.builder()
                .dbPaymentId(testPayment.getDbPaymentId())
                .order(confirmedOrder)
                .totalAmount(testPayment.getTotalAmount())
                .status(PaymentStatus.PAID)
                .build();

        given(paymentRepository.findByDbPaymentIdWithLock(paymentWithConfirmedOrder.getDbPaymentId()))
                .willReturn(Optional.of(paymentWithConfirmedOrder));

        // when & then
        assertThatThrownBy(() -> refundService.refundAll(paymentWithConfirmedOrder.getDbPaymentId(), refundRequest))
                .isInstanceOf(RefundException.class);

        verify(portOneClient, never()).refundPayment(anyString(), any());
    }

    @Test
    @DisplayName("PortOne API 호출 실패 시 예외 발생 및 실패 이력 저장")
    void refundAll_PortOneApiFailure() {
        // given
        RefundRequest refundRequest = createRefundRequest(refundReason);

        given(paymentRepository.findByDbPaymentIdWithLock(testPayment.getDbPaymentId()))
                .willReturn(Optional.of(testPayment));

        PortOneException portOneException = new PortOneException(
                HttpStatus.BAD_REQUEST,
                "PortOne API 오류"
        );
        given(portOneClient.refundPayment(eq(testPayment.getPaymentId()), any(PortOneRefundRequest.class)))
                .willThrow(portOneException);

        // when & then
        assertThatThrownBy(() -> refundService.refundAll(testPayment.getDbPaymentId(), refundRequest))
                .isInstanceOf(PortOneException.class)
                .hasMessage("PortOne API 오류");

        verify(refundHistoryService, times(1))
                .saveRequestHistory(
                        eq(testPayment.getId()),
                        eq(testPayment.getTotalAmount()),
                        eq(refundReason),
                        anyString()
                );

        verify(refundHistoryService, times(1))
                .saveFailHistory(
                        eq(testPayment.getId()),
                        eq(testPayment.getTotalAmount()),
                        eq(refundReason),
                        isNull(),
                        anyString()
                );
        verify(refundRepository, never()).save(any(Refund.class));
        verify(orderService, never()).cancelOrder(anyLong(), anyString());

        assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("PortOne 응답이 null일 때 예외 발생")
    void refundAll_PortOneResponseNull() {
        // given
        RefundRequest refundRequest = createRefundRequest(refundReason);

        given(paymentRepository.findByDbPaymentIdWithLock(testPayment.getDbPaymentId()))
                .willReturn(Optional.of(testPayment));

        given(portOneClient.refundPayment(eq(testPayment.getPaymentId()), any(PortOneRefundRequest.class)))
                .willReturn(null);

        // when & then
        assertThatThrownBy(() -> refundService.refundAll(testPayment.getDbPaymentId(), refundRequest))
                .isInstanceOf(PortOneException.class)
                .hasMessageContaining("PortOne 응답이 비어있습니다");

        verify(refundHistoryService).saveFailHistory(
                eq(testPayment.getId()),
                eq(testPayment.getTotalAmount()),
                eq(refundReason),
                isNull(),
                anyString()
        );
    }

    @Test
    @DisplayName("PortOne 응답의 Cancellation이 null일 때 예외 발생")
    void refundAll_PortOneCancellationNull() {
        // given
        RefundRequest refundRequest = createRefundRequest(refundReason);

        given(paymentRepository.findByDbPaymentIdWithLock(testPayment.getDbPaymentId()))
                .willReturn(Optional.of(testPayment));

        PortOneRefundResponse portOneResponse = new PortOneRefundResponse();

        given(portOneClient.refundPayment(eq(testPayment.getPaymentId()), any(PortOneRefundRequest.class)))
                .willReturn(portOneResponse);

        // when & then
        assertThatThrownBy(() -> refundService.refundAll(testPayment.getDbPaymentId(), refundRequest))
                .isInstanceOf(PortOneException.class)
                .hasMessageContaining("PortOne 응답이 비어있습니다");

        verify(refundHistoryService, times(1))
                .saveRequestHistory(
                        eq(testPayment.getId()),
                        eq(testPayment.getTotalAmount()),
                        eq(refundReason),
                        anyString()
                );
    }

    @Test
    @DisplayName("PortOne 환불 상태가 SUCCEEDED가 아닐 때 예외 발생")
    void refundAll_PortOneRefundFailed() {
        // given
        RefundRequest refundRequest = createRefundRequest(refundReason);

        given(paymentRepository.findByDbPaymentIdWithLock(testPayment.getDbPaymentId()))
                .willReturn(Optional.of(testPayment));

        PortOneRefundResponse portOneResponse = createFailedPortOneResponse();
        given(portOneClient.refundPayment(eq(testPayment.getPaymentId()), any(PortOneRefundRequest.class)))
                .willReturn(portOneResponse);

        // when & then
        assertThatThrownBy(() -> refundService.refundAll(testPayment.getDbPaymentId(), refundRequest))
                .isInstanceOf(PortOneException.class);

        verify(refundHistoryService, times(1))
                .saveRequestHistory(
                        eq(testPayment.getId()),
                        eq(testPayment.getTotalAmount()),
                        eq(refundReason),
                        anyString()
                );
    }

    @Test
    @DisplayName("환불 완료 처리 중 서버 내부 오류 발생 시 실패 이력 저장")
    void refundAll_InternalServerError() {
        // given
        RefundRequest refundRequest = createRefundRequest(refundReason);

        PortOneRefundResponse portOneResponse = createSuccessfulPortOneResponse();

        given(paymentRepository.findByDbPaymentIdWithLock(testPayment.getDbPaymentId()))
                .willReturn(Optional.of(testPayment));

        given(portOneClient.refundPayment(eq(testPayment.getPaymentId()), any(PortOneRefundRequest.class)))
                .willReturn(portOneResponse);

        given(refundRepository.save(any()))
                .willThrow(new RuntimeException("DB 오류"));

        // when & then
        assertThatThrownBy(() -> refundService.refundAll(testPayment.getDbPaymentId(), refundRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB 오류");

        verify(refundHistoryService, times(1))
                .saveRequestHistory(
                        eq(testPayment.getId()),
                        eq(testPayment.getTotalAmount()),
                        eq(refundReason),
                        anyString()
                );
    }

    @Test
    @DisplayName("환불 완료 후 재고 복구가 정확히 이루어지는지 검증")
    void refundAll_StockRestoration() {
        // given
        given(testUser.getUserId()).willReturn(1L);

        RefundRequest refundRequest = createRefundRequest(refundReason);

        given(paymentRepository.findByDbPaymentIdWithLock(testPayment.getDbPaymentId()))
                .willReturn(Optional.of(testPayment));

        PortOneRefundResponse portOneResponse = createSuccessfulPortOneResponse();
        given(portOneClient.refundPayment(eq(testPayment.getPaymentId()), any(PortOneRefundRequest.class)))
                .willReturn(portOneResponse);

        List<OrderProduct> orderProducts = Arrays.asList(
                createOrderProduct(200L, 3),
                createOrderProduct(201L, 5)
        );
        given(orderProductRepository.findByOrder_Id(testOrder.getId()))
                .willReturn(orderProducts);

        // when
        refundService.refundAll(testPayment.getDbPaymentId(), refundRequest);

        // then
        verify(productService, times(1)).increaseStock(200L, 3);
        verify(productService, times(1)).increaseStock(201L, 5);
    }

    @Test
    @DisplayName("환불 완료 후 멤버십 갱신이 정확히 이루어지는지 검증")
    void refundAll_MembershipUpdate() {
        // given
        given(testUser.getUserId()).willReturn(1L);

        RefundRequest refundRequest = createRefundRequest(refundReason);

        given(paymentRepository.findByDbPaymentIdWithLock(testPayment.getDbPaymentId()))
                .willReturn(Optional.of(testPayment));

        PortOneRefundResponse portOneResponse = createSuccessfulPortOneResponse();
        given(portOneClient.refundPayment(eq(testPayment.getPaymentId()), any(PortOneRefundRequest.class)))
                .willReturn(portOneResponse);

        List<OrderProduct> orderProducts = createOrderProducts(200L, 2);
        given(orderProductRepository.findByOrder_Id(testOrder.getId()))
                .willReturn(orderProducts);

        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<Long> orderIdCaptor = ArgumentCaptor.forClass(Long.class);

        // when
        refundService.refundAll(testPayment.getDbPaymentId(), refundRequest);

        // then
        verify(membershipService, times(1))
                .handleRefund(userIdCaptor.capture(), amountCaptor.capture(), orderIdCaptor.capture());

        assertAll(
                () -> assertThat(userIdCaptor.getValue()).isEqualTo(1L),
                () -> assertThat(amountCaptor.getValue()).isEqualByComparingTo(testOrder.getFinalAmount()),
                () -> assertThat(orderIdCaptor.getValue()).isEqualTo(testOrder.getId())
        );
    }

    private RefundRequest createRefundRequest(String reason) {
        RefundRequest request = new RefundRequest();
        ReflectionTestUtils.setField(request, "reason", reason);
        return request;
    }

    private PortOneRefundResponse createSuccessfulPortOneResponse() {
        PortOneRefundResponse response = new PortOneRefundResponse();
        PortOneRefundResponse.PaymentCancellation cancellation =
                new PortOneRefundResponse.PaymentCancellation();

        ReflectionTestUtils.setField(cancellation, "id", portOneRefundId);
        ReflectionTestUtils.setField(cancellation, "status", "SUCCEEDED");
        ReflectionTestUtils.setField(response, "cancellation", cancellation);

        return response;
    }

    private PortOneRefundResponse createFailedPortOneResponse() {
        PortOneRefundResponse response = new PortOneRefundResponse();
        PortOneRefundResponse.PaymentCancellation cancellation =
                new PortOneRefundResponse.PaymentCancellation();

        ReflectionTestUtils.setField(cancellation, "status", "FAILED");
        ReflectionTestUtils.setField(cancellation, "type", "INVALID_REQUEST");
        ReflectionTestUtils.setField(cancellation, "message", "환불 처리 실패");
        ReflectionTestUtils.setField(response, "cancellation", cancellation);

        return response;
    }

    private List<OrderProduct> createOrderProducts(Long productId, int count) {
        OrderProduct orderProduct = createOrderProduct(productId, count);
        return Arrays.asList(orderProduct);
    }

    private OrderProduct createOrderProduct(Long productId, int count) {
        OrderProduct orderProduct = mock(OrderProduct.class);
        given(orderProduct.getProductId()).willReturn(productId);
        given(orderProduct.getCount()).willReturn(count);
        return orderProduct;
    }
}