package com.bootcamp.paymentdemo.point.service;

import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.membership.entity.MembershipGrade;
import com.bootcamp.paymentdemo.order.consts.OrderStatus;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.point.repository.PointRepository;
import com.bootcamp.paymentdemo.point.repository.PointUsageRepository;
import com.bootcamp.paymentdemo.user.entity.User;
import com.bootcamp.paymentdemo.user.entity.UserPointBalance;
import com.bootcamp.paymentdemo.user.repository.UserPointBalanceRepository;
import com.bootcamp.paymentdemo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private PointRepository pointRepository;
    @Mock
    private PointUsageRepository pointUsageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserPointBalanceRepository userPointBalanceRepository;
    @InjectMocks
    private PointService pointService;

    private User testUser;
    private Order testOrder;
    private UserPointBalance testUserPointBalance;

    @BeforeEach
    void setUp() {
        Membership membership = Membership.builder()
                .gradeName(MembershipGrade.NORMAL)
                .accRate(BigDecimal.valueOf(1))
                .minAmount(BigDecimal.ZERO)
                .build();

        testUser = User.register("test@test.com", "1234", "test", "010-1234-5678", membership);
        ReflectionTestUtils.setField(testUser, "userId", 1L);

        testOrder = Order.builder()
                .userId("1")
                .orderNumber("ORD-20260209-0001")
                .totalAmount(BigDecimal.valueOf(10000))
                .usedPoints(BigDecimal.valueOf(400))
                .finalAmount(BigDecimal.valueOf(9600))
                .earnedPoints(BigDecimal.ZERO)
                .currency("KRW")
                .orderStatus(OrderStatus.PENDING_PAYMENT)
                .build();
        ReflectionTestUtils.setField(testOrder, "id", 1L);

        testUserPointBalance = UserPointBalance.builder()
                .user(testUser)
                .currentPoints(BigDecimal.valueOf(1000))
                .build();
        ReflectionTestUtils.setField(testUserPointBalance, "userId", 1L);
    }
}