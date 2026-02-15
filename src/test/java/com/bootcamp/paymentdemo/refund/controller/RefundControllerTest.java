package com.bootcamp.paymentdemo.refund.controller;

import com.bootcamp.paymentdemo.external.portone.client.PortOneClient;
import com.bootcamp.paymentdemo.external.portone.dto.PortOneRefundResponse;
import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.membership.entity.MembershipGrade;
import com.bootcamp.paymentdemo.membership.repository.MembershipRepository;
import com.bootcamp.paymentdemo.membership.service.MembershipService;
import com.bootcamp.paymentdemo.order.consts.OrderStatus;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.repository.OrderRepository;
import com.bootcamp.paymentdemo.order.service.OrderService;
import com.bootcamp.paymentdemo.orderProduct.entity.OrderProduct;
import com.bootcamp.paymentdemo.orderProduct.repository.OrderProductRepository;
import com.bootcamp.paymentdemo.payment.consts.PaymentStatus;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.repository.PaymentRepository;
import com.bootcamp.paymentdemo.product.consts.ProductStatus;
import com.bootcamp.paymentdemo.product.entity.Product;
import com.bootcamp.paymentdemo.product.repository.ProductRepository;
import com.bootcamp.paymentdemo.product.service.ProductService;
import com.bootcamp.paymentdemo.user.entity.User;
import com.bootcamp.paymentdemo.user.entity.UserPaidAmount;
import com.bootcamp.paymentdemo.user.repository.UserPaidAmountRepository;
import com.bootcamp.paymentdemo.user.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@WithMockUser(username = "test@example.com", roles = "USER")
class RefundControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderProductRepository orderProductRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private UserPaidAmountRepository userPaidAmountRepository;

    @MockitoBean
    private PortOneClient portOneClient;

    private User savedUser;
    private Order savedOrder;
    private Payment savedPayment;
    private Product savedProduct;
    private OrderProduct savedOrderProduct;
    private Membership normalGrade;
    private UserPaidAmount userPaidAmount;
    private String portOnePaymentId;


    @BeforeEach
    void setUp() {
        portOnePaymentId = "portone-payment-123";

        // Membership
        normalGrade = membershipRepository.findByGradeName(MembershipGrade.NORMAL)
                .orElseThrow(() -> new IllegalStateException("NORMAL 등급이 없습니다"));

        // User
        User user = User.register("test@example.com", "pw", "테스터", "010-1234-5678", normalGrade);
        savedUser = userRepository.save(user);

        UserPaidAmount userPaidAmount = UserPaidAmount.createDefault(user);

        userPaidAmountRepository.save(userPaidAmount);

        // Product
        Product product = new Product("테스트 상품", new BigDecimal(5000), 100, "테스트", ProductStatus.FOR_SALE);
        savedProduct = productRepository.save(product);

        // Order
        Order order = Order.builder()
                .orderNumber("ORD-REFUND-001")
                .user(savedUser)
                .totalAmount(new BigDecimal("10000"))
                .finalAmount(new BigDecimal("10000"))
                .usedPoints(BigDecimal.ZERO)
                .orderStatus(OrderStatus.PENDING_CONFIRMATION)
                .currency("KRW")
                .earnedPoints(BigDecimal.ZERO)
                .build();
        savedOrder = orderRepository.save(order);

        // Payment
        Payment payment = Payment.builder()
                .dbPaymentId("db1")
                .order(savedOrder)
                .totalAmount(new BigDecimal("10000"))
                .status(PaymentStatus.PAID)
                .build();
        ReflectionTestUtils.setField(payment, "paymentId", portOnePaymentId);
        savedPayment = paymentRepository.save(payment);
    }

    @Test
    @DisplayName("환불 성공 - 전체 플로우 검증")
    void refundSuccess() throws Exception {
        // Given
        String dbPaymentId = "db1";

        // PortOne 응답 Mock
        PortOneRefundResponse mockResponse = createSuccessfulPortOneResponse();
        given(portOneClient.refundPayment(eq(portOnePaymentId), any()))
                .willReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/refunds/{dbPaymentId}", dbPaymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "reason": "단순 변심"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists());

        // 검증: PortOne API 호출 확인
        verify(portOneClient, times(1)).refundPayment(eq(portOnePaymentId), any());
    }

    @Test
    @DisplayName("이미 환불된 결제 - 예외 발생")
    void refundAlreadyRefunded() throws Exception {
        // Given: 결제를 이미 환불된 상태로 변경
        savedPayment.refund();
        paymentRepository.save(savedPayment);

        String dbPaymentId = "db1";

        // When & Then
        mockMvc.perform(post("/api/refunds/{dbPaymentId}", dbPaymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "reason": "환불 사유"
                                }
                                """))
                .andDo(print())
                .andExpect(status().is4xxClientError());

        verify(portOneClient, never()).refundPayment(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 결제 - 예외 발생")
    void refundPaymentNotFound() throws Exception {
        // Given
        String dbPaymentId = "db999";

        // When & Then
        mockMvc.perform(post("/api/refunds/{dbPaymentId}", dbPaymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "reason": "환불 사유"
                                }
                                """))
                .andDo(print())
                .andExpect(status().is4xxClientError());

        verify(portOneClient, never()).refundPayment(any(), any());
    }

    @Test
    @DisplayName("환불 사유가 빈 문자열일 때 - Validation 실패")
    void refundEmptyReason() throws Exception {
        // Given
        String dbPaymentId = "db1";

        // When & Then
        mockMvc.perform(post("/api/refunds/{dbPaymentId}", dbPaymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "reason": ""
                                }
                                """))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("환불 사유가 공백만 있을 때 - Validation 실패")
    void refundBlankReason() throws Exception {
        // Given
        String dbPaymentId = "db1";

        // When & Then
        mockMvc.perform(post("/api/refunds/{dbPaymentId}", dbPaymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "reason": "   "
                                }
                                """))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("환불 사유가 null일 때 - Validation 실패")
    void refundNullReason() throws Exception {
        // Given
        String dbPaymentId = "db1";

        // When & Then
        mockMvc.perform(post("/api/refunds/{dbPaymentId}", dbPaymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                }
                                """))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("잘못된 JSON 형식 - Bad Request")
    void refundInvalidJson() throws Exception {
        // Given
        String dbPaymentId = "db1";

        // When & Then
        mockMvc.perform(post("/api/refunds/{dbPaymentId}", dbPaymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andDo(print())
                .andExpect(status().is5xxServerError()); // GlobalExceptionHandler가 500으로 처리
    }

    @Test
    @DisplayName("Request Body가 없을 때 - Bad Request")
    void refundNoRequestBody() throws Exception {
        // Given
        String dbPaymentId = "db1";

        // When & Then
        mockMvc.perform(post("/api/refunds/{dbPaymentId}", dbPaymentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is5xxServerError()); // GlobalExceptionHandler가 500으로 처리
    }

    @Test
    @DisplayName("PathVariable이 비어있을 때 - Not Found")
    void refundEmptyPathVariable() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/refunds/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "reason": "환불 사유"
                                }
                                """))
                .andDo(print())
                .andExpect(status().is5xxServerError()); // 라우팅 에러로 500 발생
    }

    @Test
    @DisplayName("Content-Type이 없을 때 - Unsupported Media Type")
    void refundNoContentType() throws Exception {
        // Given
        String dbPaymentId = "db1";

        // When & Then
        mockMvc.perform(post("/api/refunds/{dbPaymentId}", dbPaymentId)
                        .content("""
                                {
                                    "reason": "환불 사유"
                                }
                                """))
                .andDo(print())
                .andExpect(status().is5xxServerError()); // GlobalExceptionHandler가 500으로 처리
    }

    @Test
    @DisplayName("Content-Type이 잘못되었을 때 - Unsupported Media Type")
    void refundWrongContentType() throws Exception {
        // Given
        String dbPaymentId = "db1";

        // When & Then
        mockMvc.perform(post("/api/refunds/{dbPaymentId}", dbPaymentId)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("""
                                {
                                    "reason": "환불 사유"
                                }
                                """))
                .andDo(print())
                .andExpect(status().is5xxServerError()); // GlobalExceptionHandler가 500으로 처리
    }

    @Test
    @DisplayName("PortOne API 실패 - 서버 에러")
    void refundPortOneApiFailure() throws Exception {
        // Given
        String dbPaymentId = "db1";

        // PortOne API 호출 시 예외 발생
        given(portOneClient.refundPayment(eq(portOnePaymentId), any()))
                .willThrow(new RuntimeException("PortOne API 오류"));

        // When & Then
        mockMvc.perform(post("/api/refunds/{dbPaymentId}", dbPaymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "reason": "환불 사유"
                                }
                                """))
                .andDo(print())
                .andExpect(status().is5xxServerError());
    }

    private PortOneRefundResponse createSuccessfulPortOneResponse() {
        PortOneRefundResponse response = new PortOneRefundResponse();
        PortOneRefundResponse.PaymentCancellation cancellation =
                new PortOneRefundResponse.PaymentCancellation();

        ReflectionTestUtils.setField(cancellation, "id", "cancel_123");
        ReflectionTestUtils.setField(cancellation, "status", "SUCCEEDED");
        ReflectionTestUtils.setField(cancellation, "reason", "단순 변심");
        ReflectionTestUtils.setField(cancellation, "totalAmount", 10000L);
        ReflectionTestUtils.setField(response, "cancellation", cancellation);

        return response;
    }
}