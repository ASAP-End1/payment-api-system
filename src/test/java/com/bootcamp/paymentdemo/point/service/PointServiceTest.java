package com.bootcamp.paymentdemo.point.service;

import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.membership.entity.MembershipGrade;
import com.bootcamp.paymentdemo.order.consts.OrderStatus;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.point.dto.PointGetResponse;
import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import com.bootcamp.paymentdemo.point.entity.PointType;
import com.bootcamp.paymentdemo.point.repository.PointRepository;
import com.bootcamp.paymentdemo.point.repository.PointUsageRepository;
import com.bootcamp.paymentdemo.user.entity.User;
import com.bootcamp.paymentdemo.user.entity.UserPointBalance;
import com.bootcamp.paymentdemo.user.exception.UserNotFoundException;
import com.bootcamp.paymentdemo.user.repository.UserPointBalanceRepository;
import com.bootcamp.paymentdemo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

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


    // 포인트 내역 조회 테스트
    @Test
    @DisplayName("포인트 내역 조회 - 성공")
    void getPointHistory_성공() {
        // Given
        PointTransaction spent = new PointTransaction(testUser, testOrder, BigDecimal.valueOf(400).negate(), PointType.SPENT);
        PointTransaction refunded = new PointTransaction(testUser, testOrder, BigDecimal.valueOf(400), PointType.REFUNDED);
        PointTransaction earned = new PointTransaction(testUser, testOrder, BigDecimal.valueOf(200), PointType.EARNED);
        PointTransaction canceled = new PointTransaction(testUser, testOrder, BigDecimal.valueOf(200).negate(), PointType.CANCELED);
        PointTransaction expired = new PointTransaction(testUser, testOrder, BigDecimal.valueOf(100).negate(), PointType.EXPIRED);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
        when(pointRepository.findByUser_UserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(spent, refunded, earned, canceled, expired));

        // When
        List<PointGetResponse> result = pointService.getPointHistory("test@test.com");

        // Then
        assertThat(result).hasSize(5);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(400).negate());
        assertThat(result.get(1).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(400));
        assertThat(result.get(2).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(result.get(3).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200).negate());
        assertThat(result.get(4).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100).negate());
    }

    @Test
    @DisplayName("포인트 내역 조회 - 실패")
    void getPointHistory_실패() {
        // Given
        when(userRepository.findByEmail("fail@test.com")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class,
                () -> pointService.getPointHistory("fail@test.com"));
    }
}