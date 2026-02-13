package com.bootcamp.paymentdemo.order.controller;

import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.membership.entity.MembershipGrade;
import com.bootcamp.paymentdemo.membership.repository.MembershipRepository;
import com.bootcamp.paymentdemo.order.dto.OrderCreateRequest;
import com.bootcamp.paymentdemo.order.dto.OrderProductRequest;
import com.bootcamp.paymentdemo.order.service.OrderService;
import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import com.bootcamp.paymentdemo.point.repository.PointRepository;
import com.bootcamp.paymentdemo.point.consts.PointType;
import com.bootcamp.paymentdemo.product.consts.ProductStatus;
import com.bootcamp.paymentdemo.product.entity.Product;
import com.bootcamp.paymentdemo.product.repository.ProductRepository;
import com.bootcamp.paymentdemo.user.entity.User;
import com.bootcamp.paymentdemo.user.entity.UserGradeHistory;
import com.bootcamp.paymentdemo.user.entity.UserPaidAmount;
import com.bootcamp.paymentdemo.user.entity.UserPointBalance;
import com.bootcamp.paymentdemo.user.repository.UserGradeHistoryRepository;
import com.bootcamp.paymentdemo.user.repository.UserPaidAmountRepository;
import com.bootcamp.paymentdemo.user.repository.UserPointBalanceRepository;
import com.bootcamp.paymentdemo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class OrderControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private UserPaidAmountRepository userPaidAmountRepository;

    @Autowired
    private UserGradeHistoryRepository userGradeHistoryRepository;

    @Autowired
    private UserPointBalanceRepository userPointBalanceRepository;

    private User testUser;
    private Product testProduct1;
    private Product testProduct2;
    private Membership normalGrade;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        normalGrade = membershipRepository.findByGradeName(MembershipGrade.NORMAL)
                .orElseThrow(() -> new IllegalStateException("NORMAL 등급이 없습니다"));

        // 테스트 사용자 생성
        testUser = User.register(
                "ordercontroller@example.com",
                "encodedPassword",
                "주문컨트롤러테스트",
                "010-9999-9999",
                normalGrade
        );
        testUser = userRepository.save(testUser);

        // 총 결제 금액 초기화
        UserPaidAmount paidAmount = UserPaidAmount.createDefault(testUser);
        userPaidAmountRepository.save(paidAmount);

        // 초기 등급 이력 생성
        UserGradeHistory initialHistory = UserGradeHistory.createInitial(testUser, normalGrade);
        userGradeHistoryRepository.save(initialHistory);

        // 포인트 잔액 초기화
        UserPointBalance pointBalance = UserPointBalance.createDefault(testUser);
        userPointBalanceRepository.save(pointBalance);

        // 포인트 초기화 (10,000원)
        PointTransaction initialPoint = new PointTransaction(
                testUser,
                null,
                new BigDecimal("10000"),
                PointType.EARNED
        );
        pointRepository.save(initialPoint);

        // 테스트 상품 생성
        testProduct1 = new Product(
                "컨트롤러 테스트 상품 1",
                new BigDecimal("5000"),
                100,
                "테스트 카테고리",
                ProductStatus.FOR_SALE
        );
        testProduct1 = productRepository.save(testProduct1);

        testProduct2 = new Product(
                "컨트롤러 테스트 상품 2",
                new BigDecimal("10000"),
                50,
                "테스트 카테고리",
                ProductStatus.FOR_SALE
        );
        testProduct2 = productRepository.save(testProduct2);
    }

    @Test
    @DisplayName("POST /api/orders - 주문 생성 성공")
    @WithMockUser(username = "ordercontroller@example.com")
    void createOrder_Success() throws Exception {
        // given
        String requestJson = String.format("""
                {
                    "userId": %d,
                    "usedPoints": 0,
                    "items": [
                        {
                            "productId": %d,
                            "quantity": 2
                        }
                    ]
                }
                """, testUser.getUserId(), testProduct1.getId());

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("주문 생성 성공"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.orderNumber").isNotEmpty())
                .andExpect(jsonPath("$.data.totalAmount").value(10000))
                .andExpect(jsonPath("$.data.usedPoints").value(0))
                .andExpect(jsonPath("$.data.finalAmount").value(10000))
                .andExpect(jsonPath("$.data.earnedPoints").value(100))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));
    }

    @Test
    @DisplayName("POST /api/orders - 주문 생성 성공 (포인트 사용)")
    @WithMockUser(username = "ordercontroller@example.com")
    void createOrder_Success_WithPoints() throws Exception {
        // given
        String requestJson = String.format("""
                {
                    "userId": %d,
                    "usedPoints": 3000,
                    "items": [
                        {
                            "productId": %d,
                            "quantity": 2
                        }
                    ]
                }
                """, testUser.getUserId(), testProduct1.getId());

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalAmount").value(10000))
                .andExpect(jsonPath("$.data.usedPoints").value(3000))
                .andExpect(jsonPath("$.data.finalAmount").value(7000))
                .andExpect(jsonPath("$.data.earnedPoints").value(70));
    }

    @Test
    @DisplayName("GET /api/orders - 주문 목록 조회 성공")
    @WithMockUser(username = "ordercontroller@example.com")
    void getAllOrders_Success() throws Exception {
        // given - 주문 생성
        List<OrderProductRequest> items = List.of(
                new OrderProductRequest(testProduct1.getId(), 1)
        );
        OrderCreateRequest request = new OrderCreateRequest(
                testUser.getUserId(),
                BigDecimal.ZERO,
                items
        );
        orderService.createOrder(request, testUser.getEmail());

        // when & then
        mockMvc.perform(get("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("주문 목록 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/orders/{orderId} - 주문 상세 조회 성공")
    @WithMockUser(username = "ordercontroller@example.com")
    void getOneOrder_Success() throws Exception {
        // given - 주문 생성
        List<OrderProductRequest> items = List.of(
                new OrderProductRequest(testProduct1.getId(), 2),
                new OrderProductRequest(testProduct2.getId(), 1)
        );
        OrderCreateRequest request = new OrderCreateRequest(
                testUser.getUserId(),
                BigDecimal.ZERO,
                items
        );
        var createResponse = orderService.createOrder(request, testUser.getEmail());

        // when & then
        mockMvc.perform(get("/api/orders/{orderId}", createResponse.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("주문 상세 조회 성공"))
                .andExpect(jsonPath("$.data.orderId").value(createResponse.getId()))
                .andExpect(jsonPath("$.data.orderNumber").value(createResponse.getOrderNumber()))
                .andExpect(jsonPath("$.data.totalAmount").value(20000))
                .andExpect(jsonPath("$.data.orderProducts").isArray())
                .andExpect(jsonPath("$.data.orderProducts", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/orders/{orderId} - 존재하지 않는 주문 조회 실패")
    @WithMockUser(username = "ordercontroller@example.com")
    void getOneOrder_NotFound() throws Exception {
        // when & then
        mockMvc.perform(get("/api/orders/{orderId}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/orders/{orderId}/confirm - 주문 확정 성공")
    @WithMockUser(username = "ordercontroller@example.com")
    void confirmOrder_Success() throws Exception {
        // given - 주문 생성 및 결제 완료
        List<OrderProductRequest> items = List.of(
                new OrderProductRequest(testProduct1.getId(), 2)
        );
        OrderCreateRequest request = new OrderCreateRequest(
                testUser.getUserId(),
                BigDecimal.ZERO,
                items
        );
        var createResponse = orderService.createOrder(request, testUser.getEmail());
        orderService.completePayment(createResponse.getId());

        // when & then
        mockMvc.perform(patch("/api/orders/{orderId}/confirm", createResponse.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("주문 확정 성공"));
    }

    @Test
    @DisplayName("PATCH /api/orders/{orderId}/confirm - 결제 미완료 주문 확정 실패")
    @WithMockUser(username = "ordercontroller@example.com")
    void confirmOrder_Fail_NotPaid() throws Exception {
        // given - 주문 생성만 (결제 미완료)
        List<OrderProductRequest> items = List.of(
                new OrderProductRequest(testProduct1.getId(), 1)
        );
        OrderCreateRequest request = new OrderCreateRequest(
                testUser.getUserId(),
                BigDecimal.ZERO,
                items
        );
        var createResponse = orderService.createOrder(request, testUser.getEmail());

        // when & then
        mockMvc.perform(patch("/api/orders/{orderId}/confirm", createResponse.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
