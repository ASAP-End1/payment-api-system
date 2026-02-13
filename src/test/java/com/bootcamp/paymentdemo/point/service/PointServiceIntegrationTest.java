package com.bootcamp.paymentdemo.point.service;

import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.membership.entity.MembershipGrade;
import com.bootcamp.paymentdemo.membership.repository.MembershipRepository;
import com.bootcamp.paymentdemo.order.consts.OrderStatus;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.repository.OrderRepository;
import com.bootcamp.paymentdemo.point.consts.PointType;
import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import com.bootcamp.paymentdemo.point.entity.PointUsage;
import com.bootcamp.paymentdemo.point.repository.PointRepository;
import com.bootcamp.paymentdemo.point.repository.PointUsageRepository;
import com.bootcamp.paymentdemo.user.entity.User;
import com.bootcamp.paymentdemo.user.entity.UserPointBalance;
import com.bootcamp.paymentdemo.user.repository.UserPointBalanceRepository;
import com.bootcamp.paymentdemo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PointServiceIntegrationTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private PointUsageRepository pointUsageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserPointBalanceRepository userPointBalanceRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    private User testUser;
    private UserPointBalance testUserPointBalance;
    private Membership normalGrade;

    @BeforeEach
    void setUp() {
        normalGrade = membershipRepository.findByGradeName(MembershipGrade.NORMAL).orElseThrow(
                () -> new IllegalStateException("NORMAL 등급이 없습니다")
        );

        // 테스트 사용자 생성
        testUser = User.register(
                "test@test.com",
                "encodedPassword",
                "test",
                "010-1234-5678",
                normalGrade
        );
        userRepository.save(testUser);

        // 테스트 포인트 잔액 생성
        testUserPointBalance = UserPointBalance.builder()
                .user(testUser)
                .currentPoints(BigDecimal.ZERO)
                .build();
        userPointBalanceRepository.save(testUserPointBalance);
    }

    @Test
    @DisplayName("포인트 적립 -> 사용 -> 환불 통합 테스트")
    void pointIntegrationTest() {
        // given - 포인트 적립
        // 20000, 30000원 결제 -> 200포인트, 300포인트 적립
        Order earnOrder1 = createOrder(BigDecimal.valueOf(20000), BigDecimal.ZERO, BigDecimal.valueOf(200));
        Order earnOrder2 = createOrder(BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.valueOf(300));
        pointService.earnPoints(testUser, earnOrder1);
        pointService.earnPoints(testUser, earnOrder2);

        // then
        assertThat(pointService.checkPointBalance(testUser)).isEqualByComparingTo(BigDecimal.valueOf(500));

        // when
        // 400포인트 사용
        Order useOrder = createOrder(BigDecimal.valueOf(10000), BigDecimal.valueOf(400), BigDecimal.valueOf(96));
        pointService.usePoints(testUser, useOrder);

        // then
        assertThat(pointService.checkPointBalance(testUser)).isEqualByComparingTo(BigDecimal.valueOf(100));

        PointTransaction earned1 = pointRepository.findByOrderIdAndType(earnOrder1.getId(), PointType.EARNED).get();
        PointTransaction earned2 = pointRepository.findByOrderIdAndType(earnOrder2.getId(), PointType.EARNED).get();
        assertThat(earned1.getRemainingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(earned2.getRemainingAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));

        List<PointUsage> usages = pointUsageRepository.findByOrderId(useOrder.getId());
        assertThat(usages).hasSize(2);
        assertThat(usages.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(usages.get(1).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));

        // when
        // 환불 -> 400포인트 복구
        pointService.refundPoints(testUser, useOrder);

        // then
        assertThat(pointService.checkPointBalance(testUser)).isEqualByComparingTo(BigDecimal.valueOf(500));

        assertThat(earned1.getRemainingAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(earned2.getRemainingAmount()).isEqualByComparingTo(BigDecimal.valueOf(300));
    }

    @Test
    @DisplayName("포인트 소멸 테스트")
    void expirePoints() {
        // given
        Order earnOrder = createOrder(BigDecimal.valueOf(10000), BigDecimal.ZERO, BigDecimal.valueOf(100));
        pointService.earnPoints(testUser, earnOrder);

        // 포인트 만료일 어제로 설정
        PointTransaction earned = pointRepository.findByOrderIdAndType(earnOrder.getId(), PointType.EARNED).get();
        ReflectionTestUtils.setField(earned, "expiresAt", LocalDate.now().minusDays(1));
        pointRepository.save(earned);

        // when
        pointService.expirePoints();

        // then
        assertThat(pointService.checkPointBalance(testUser)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(earned.getRemainingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("포인트 정합성 보정 테스트")
    void syncPointBalance() {
        // given
        Order earnOrder = createOrder(BigDecimal.valueOf(10000), BigDecimal.ZERO, BigDecimal.valueOf(100));
        pointService.earnPoints(testUser, earnOrder);

        // 포인트 잔액 잘못된 값으로 설정
        UserPointBalance userPointBalance = userPointBalanceRepository.findByUserId(testUser.getUserId()).get();
        ReflectionTestUtils.setField(userPointBalance, "currentPoints", BigDecimal.valueOf(150));
        userPointBalanceRepository.save(userPointBalance);

        // when
        pointService.syncPointBalance();

        // then
        assertThat(userPointBalance.getCurrentPoints()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    // 주문 생성 메서드
    private Order createOrder(BigDecimal totalAmount, BigDecimal usedPoints, BigDecimal earnedPoints) {
        Order order = Order.builder()
                .user(testUser)
                .orderNumber("ORD-" + System.currentTimeMillis())
                .totalAmount(totalAmount)
                .usedPoints(usedPoints)
                .finalAmount(totalAmount.subtract(usedPoints))
                .earnedPoints(earnedPoints)
                .currency("KRW")
                .orderStatus(OrderStatus.PENDING_CONFIRMATION)
                .build();
        return orderRepository.save(order);
    }
}