package com.bootcamp.paymentdemo.payment.controller;

import com.bootcamp.paymentdemo.external.portone.client.PortOneClient;
import com.bootcamp.paymentdemo.external.portone.dto.PortOnePaymentResponse;
import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.membership.entity.MembershipGrade;
import com.bootcamp.paymentdemo.membership.repository.MembershipRepository;
import com.bootcamp.paymentdemo.order.consts.OrderStatus;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.repository.OrderRepository;
import com.bootcamp.paymentdemo.payment.consts.PaymentStatus;
import com.bootcamp.paymentdemo.payment.dto.PaymentCreateRequest;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.repository.PaymentRepository;
import com.bootcamp.paymentdemo.user.entity.User;
import com.bootcamp.paymentdemo.user.repository.UserRepository;
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
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@WithMockUser(username = "test@example.com", roles = "USER")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @MockitoBean
    private PortOneClient portOneClient;

    private User savedUser;
    private Order savedOrder;
    private Membership normalGrade;

    @BeforeEach
    void setUp() {

        normalGrade = membershipRepository.findByGradeName(MembershipGrade.NORMAL)
                .orElseThrow(() -> new IllegalStateException("NORMAL 등급이 없습니다"));


        User user = User.register("test@example.com", "pw", "테스터", "010-1234-5678", normalGrade);
        savedUser = userRepository.save(user);

        Order order = Order.builder()
                .orderNumber("ORD-INTEG-001")
                .user(savedUser)
                .totalAmount(new BigDecimal("10000"))
                .finalAmount(new BigDecimal("10000"))
                .usedPoints(BigDecimal.ZERO)
                .orderStatus(OrderStatus.PENDING_PAYMENT)
                .currency("KRW")
                .earnedPoints(BigDecimal.ZERO)
                .build();
        savedOrder = orderRepository.save(order);
    }

    @Test
    @DisplayName("결제 생성 통합 테스트")
    void createPayment() throws Exception {
        // Given
        PaymentCreateRequest request = new PaymentCreateRequest();
        ReflectionTestUtils.setField(request, "orderNumber", "ORD-INTEG-001");
        ReflectionTestUtils.setField(request, "totalAmount", new BigDecimal("10000"));
        ReflectionTestUtils.setField(request, "pointsToUse", BigDecimal.ZERO);

        String jsonRequest = objectMapper.writeValueAsString(request);

        // When
        mockMvc.perform(post("/api/payment/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andDo(print())
                .andExpect(status().isCreated());

        // Then
        Payment savedPayment = paymentRepository.findAll().stream()
                .filter(p -> p.getOrder().getOrderNumber().equals("ORD-INTEG-001"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("저장된 결제 정보를 찾을 수 없습니다."));

        assertThat(savedPayment.getTotalAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("결제 확정 통합 테스트")
    void confirmPayment() throws Exception {
        // Given
        String dbPaymentId = "imp_test_1234";
        Payment pendingPayment = Payment.builder()
                .dbPaymentId(dbPaymentId)
                .order(savedOrder)
                .totalAmount(new BigDecimal("10000"))
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(pendingPayment);

        PortOnePaymentResponse mockResponse = mock(PortOnePaymentResponse.class);
        PortOnePaymentResponse.Amount mockAmount = mock(PortOnePaymentResponse.Amount.class);

        given(mockAmount.total()).willReturn(new BigDecimal("10000"));
        given(mockResponse.amount()).willReturn(mockAmount);
        given(portOneClient.getPayment(dbPaymentId)).willReturn(mockResponse);

        // When
        mockMvc.perform(post("/api/payment/" + dbPaymentId + "/confirm")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        // Then
        Payment confirmedPayment = paymentRepository.findByDbPaymentIdWithLock(dbPaymentId)
                .orElseThrow(() -> new AssertionError("결제 정보를 찾을 수 없습니다."));
        assertThat(confirmedPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }
}