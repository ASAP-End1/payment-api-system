package com.bootcamp.paymentdemo.order.service;

import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.membership.entity.MembershipGrade;
import com.bootcamp.paymentdemo.membership.repository.MembershipRepository;
import com.bootcamp.paymentdemo.order.consts.OrderStatus;
import com.bootcamp.paymentdemo.order.dto.OrderCreateRequest;
import com.bootcamp.paymentdemo.order.dto.OrderCreateResponse;
import com.bootcamp.paymentdemo.order.dto.OrderGetDetailResponse;
import com.bootcamp.paymentdemo.order.dto.OrderProductRequest;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.repository.OrderRepository;
import com.bootcamp.paymentdemo.point.consts.PointType;
import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import com.bootcamp.paymentdemo.point.repository.PointRepository;
import com.bootcamp.paymentdemo.point.service.PointService;
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
import com.bootcamp.paymentdemo.order.exception.OrderNotFoundException;
import com.bootcamp.paymentdemo.product.exception.ProductNotFoundException;
import com.bootcamp.paymentdemo.user.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private PointService pointService;

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
        normalGrade = membershipRepository.findByGradeName(MembershipGrade.NORMAL)
                .orElseThrow(() -> new IllegalStateException("NORMAL 등급이 없습니다"));


        testUser = User.register(
                "ordertest@example.com",
                "encodedPassword",
                "주문테스트유저",
                "010-1234-5678",
                normalGrade
        );
        testUser = userRepository.save(testUser);


        UserPaidAmount paidAmount = UserPaidAmount.createDefault(testUser);
        userPaidAmountRepository.save(paidAmount);


        UserGradeHistory initialHistory = UserGradeHistory.createInitial(testUser, normalGrade);
        userGradeHistoryRepository.save(initialHistory);


        UserPointBalance pointBalance = UserPointBalance.createDefault(testUser);
        userPointBalanceRepository.save(pointBalance);


        PointTransaction initialPoint = new PointTransaction(
                testUser,
                null,
                new BigDecimal("10000"),
                PointType.EARNED
        );
        pointRepository.save(initialPoint);


        testProduct1 = new Product(
                "테스트 주문 상품 1",
                new BigDecimal("5000"),
                100,
                "테스트 카테고리",
                ProductStatus.FOR_SALE
        );
        testProduct1 = productRepository.save(testProduct1);

        testProduct2 = new Product(
                "테스트 주문 상품 2",
                new BigDecimal("10000"),
                50,
                "테스트 카테고리",
                ProductStatus.FOR_SALE
        );
        testProduct2 = productRepository.save(testProduct2);
    }

    @Test
    @DisplayName("주문 생성 성공 - 포인트 미사용")
    void createOrder_Success_WithoutPoints() {

        List<OrderProductRequest> items = List.of(
                new OrderProductRequest(testProduct1.getId(), 2)
        );
        OrderCreateRequest request = new OrderCreateRequest(
                testUser.getUserId(),
                BigDecimal.ZERO,
                items
        );


        OrderCreateResponse response = orderService.createOrder(request, testUser.getEmail());


        assertThat(response.getId()).isNotNull();
        assertThat(response.getOrderNumber()).isNotNull();
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(response.getUsedPoints()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getFinalAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(response.getEarnedPoints()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(response.getStatus()).isEqualTo("PENDING_PAYMENT");


        Product updatedProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(98);
    }

    @Test
    @DisplayName("주문 생성 성공 - 포인트 사용")
    void createOrder_Success_WithPoints() {

        List<OrderProductRequest> items = List.of(
                new OrderProductRequest(testProduct1.getId(), 2)
        );
        OrderCreateRequest request = new OrderCreateRequest(
                testUser.getUserId(),
                new BigDecimal("3000"),
                items
        );


        OrderCreateResponse response = orderService.createOrder(request, testUser.getEmail());


        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(response.getUsedPoints()).isEqualByComparingTo(new BigDecimal("3000"));
        assertThat(response.getFinalAmount()).isEqualByComparingTo(new BigDecimal("7000"));
        assertThat(response.getEarnedPoints()).isEqualByComparingTo(new BigDecimal("70"));
    }

    @Test
    @DisplayName("주문 생성 실패 - 포인트 잔액 부족")
    void createOrder_Fail_InsufficientPoints() {

        List<OrderProductRequest> items = List.of(
                new OrderProductRequest(testProduct1.getId(), 1)
        );
        OrderCreateRequest request = new OrderCreateRequest(
                testUser.getUserId(),
                new BigDecimal("20000"),
                items
        );


        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(request, testUser.getEmail());
        });
    }

    @Test
    @DisplayName("주문 생성 실패 - 재고 부족")
    void createOrder_Fail_InsufficientStock() {

        List<OrderProductRequest> items = List.of(
                new OrderProductRequest(testProduct1.getId(), 200)
        );
        OrderCreateRequest request = new OrderCreateRequest(
                testUser.getUserId(),
                BigDecimal.ZERO,
                items
        );


        assertThrows(IllegalStateException.class, () -> {
            orderService.createOrder(request, testUser.getEmail());
        });
    }

    @Test
    @DisplayName("주문 생성 실패 - 존재하지 않는 사용자")
    void createOrder_Fail_UserNotFound() {

        List<OrderProductRequest> items = List.of(
                new OrderProductRequest(testProduct1.getId(), 1)
        );
        OrderCreateRequest request = new OrderCreateRequest(
                999999L,
                BigDecimal.ZERO,
                items
        );


        assertThrows(UserNotFoundException.class, () -> {
            orderService.createOrder(request, "notexist@example.com");
        });
    }

    @Test
    @DisplayName("주문 생성 실패 - 존재하지 않는 상품")
    void createOrder_Fail_ProductNotFound() {

        List<OrderProductRequest> items = List.of(
                new OrderProductRequest(999999L, 1)
        );
        OrderCreateRequest request = new OrderCreateRequest(
                testUser.getUserId(),
                BigDecimal.ZERO,
                items
        );


        assertThrows(ProductNotFoundException.class, () -> {
            orderService.createOrder(request, testUser.getEmail());
        });
    }

    @Test
    @DisplayName("주문 목록 조회 성공")
    void findAllOrders_Success() {

        List<OrderProductRequest> items = List.of(
                new OrderProductRequest(testProduct1.getId(), 1)
        );
        OrderCreateRequest request = new OrderCreateRequest(
                testUser.getUserId(),
                BigDecimal.ZERO,
                items
        );
        orderService.createOrder(request, testUser.getEmail());


        List<OrderGetDetailResponse> orders = orderService.findAllOrders(testUser.getEmail());


        assertThat(orders).isNotEmpty();
        assertThat(orders).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("주문 상세 조회 성공")
    void findOrderDetail_Success() {

        List<OrderProductRequest> items = List.of(
                new OrderProductRequest(testProduct1.getId(), 2),
                new OrderProductRequest(testProduct2.getId(), 1)
        );
        OrderCreateRequest request = new OrderCreateRequest(
                testUser.getUserId(),
                BigDecimal.ZERO,
                items
        );
        OrderCreateResponse createResponse = orderService.createOrder(request, testUser.getEmail());


        OrderGetDetailResponse response = orderService.findOrderDetail(createResponse.getId());


        assertThat(response.getOrderId()).isEqualTo(createResponse.getId());
        assertThat(response.getOrderNumber()).isEqualTo(createResponse.getOrderNumber());
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("20000"));
        assertThat(response.getOrderProducts()).hasSize(2);
    }

    @Test
    @DisplayName("주문 상세 조회 실패 - 존재하지 않는 주문")
    void findOrderDetail_NotFound() {

        assertThrows(OrderNotFoundException.class, () -> {
            orderService.findOrderDetail(999999L);
        });
    }

    @Test
    @DisplayName("결제 완료 처리 성공 - 포인트 차감")
    void completePayment_Success() {

        List<OrderProductRequest> items = List.of(
                new OrderProductRequest(testProduct1.getId(), 2)
        );
        OrderCreateRequest request = new OrderCreateRequest(
                testUser.getUserId(),
                new BigDecimal("3000"),
                items
        );
        OrderCreateResponse createResponse = orderService.createOrder(request, testUser.getEmail());


        orderService.completePayment(createResponse.getId());


        Order updatedOrder = orderRepository.findById(createResponse.getId()).orElseThrow();
        assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);


        BigDecimal currentBalance = pointService.checkPointBalance(testUser);
        assertThat(currentBalance).isEqualByComparingTo(new BigDecimal("7000"));
    }

    @Test
    @DisplayName("결제 완료 처리 실패 - 이미 결제 완료된 주문")
    void completePayment_Fail_AlreadyCompleted() {

        List<OrderProductRequest> items = List.of(
                new OrderProductRequest(testProduct1.getId(), 1)
        );
        OrderCreateRequest request = new OrderCreateRequest(
                testUser.getUserId(),
                BigDecimal.ZERO,
                items
        );
        OrderCreateResponse createResponse = orderService.createOrder(request, testUser.getEmail());
        orderService.completePayment(createResponse.getId());


        assertThrows(IllegalStateException.class, () -> {
            orderService.completePayment(createResponse.getId());
        });
    }

    @Test
    @DisplayName("주문 확정 성공 - 포인트 적립")
    void confirmOrder_Success() {

        List<OrderProductRequest> items = List.of(
                new OrderProductRequest(testProduct1.getId(), 2)
        );
        OrderCreateRequest request = new OrderCreateRequest(
                testUser.getUserId(),
                BigDecimal.ZERO,
                items
        );
        OrderCreateResponse createResponse = orderService.createOrder(request, testUser.getEmail());
        orderService.completePayment(createResponse.getId());


        orderService.confirmOrder(createResponse.getId());


        Order confirmedOrder = orderRepository.findById(createResponse.getId()).orElseThrow();
        assertThat(confirmedOrder.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);


        BigDecimal currentBalance = pointService.checkPointBalance(testUser);
        assertThat(currentBalance).isEqualByComparingTo(new BigDecimal("10100"));
    }

    @Test
    @DisplayName("주문 확정 실패 - 결제 미완료 주문")
    void confirmOrder_Fail_NotPaid() {

        List<OrderProductRequest> items = List.of(
                new OrderProductRequest(testProduct1.getId(), 1)
        );
        OrderCreateRequest request = new OrderCreateRequest(
                testUser.getUserId(),
                BigDecimal.ZERO,
                items
        );
        OrderCreateResponse createResponse = orderService.createOrder(request, testUser.getEmail());


        assertThrows(IllegalStateException.class, () -> {
            orderService.confirmOrder(createResponse.getId());
        });
    }

    @Test
    @DisplayName("주문 취소 성공 - 포인트 복구")
    void cancelOrder_Success() {

        List<OrderProductRequest> items = List.of(
                new OrderProductRequest(testProduct1.getId(), 2)
        );
        OrderCreateRequest request = new OrderCreateRequest(
                testUser.getUserId(),
                new BigDecimal("3000"),
                items
        );
        OrderCreateResponse createResponse = orderService.createOrder(request, testUser.getEmail());
        orderService.completePayment(createResponse.getId());


        orderService.cancelOrder(createResponse.getId(), "단순 변심");


        Order cancelledOrder = orderRepository.findById(createResponse.getId()).orElseThrow();
        assertThat(cancelledOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);


        BigDecimal currentBalance = pointService.checkPointBalance(testUser);
        assertThat(currentBalance).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("주문 취소 실패 - 이미 확정된 주문")
    void cancelOrder_Fail_AlreadyConfirmed() {

        List<OrderProductRequest> items = List.of(
                new OrderProductRequest(testProduct1.getId(), 1)
        );
        OrderCreateRequest request = new OrderCreateRequest(
                testUser.getUserId(),
                BigDecimal.ZERO,
                items
        );
        OrderCreateResponse createResponse = orderService.createOrder(request, testUser.getEmail());
        orderService.completePayment(createResponse.getId());
        orderService.confirmOrder(createResponse.getId());


        assertThrows(IllegalStateException.class, () -> {
            orderService.cancelOrder(createResponse.getId(), "단순 변심");
        });
    }
}
