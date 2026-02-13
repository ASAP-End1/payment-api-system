package com.bootcamp.paymentdemo.point.service;

import com.bootcamp.paymentdemo.common.dto.PageResponse;
import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.membership.entity.MembershipGrade;
import com.bootcamp.paymentdemo.order.consts.OrderStatus;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.point.dto.PointGetResponse;
import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import com.bootcamp.paymentdemo.point.consts.PointType;
import com.bootcamp.paymentdemo.point.entity.PointUsage;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
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
    void setUp() throws Exception {
        Membership membership = mock(Membership.class);
        ReflectionTestUtils.setField(membership, "gradeName", MembershipGrade.NORMAL);
        ReflectionTestUtils.setField(membership, "accRate", BigDecimal.valueOf(1));
        ReflectionTestUtils.setField(membership, "minAmount", BigDecimal.ZERO);

        testUser = User.register(
                "test@test.com",
                "encodedPassword",
                "test",
                "010-1234-5678",
                membership);
        ReflectionTestUtils.setField(testUser, "userId", 1L);

        testOrder = Order.builder()
                .user(testUser)
                .orderNumber("ORD-20260209-0001")
                .totalAmount(BigDecimal.valueOf(10000))
                .usedPoints(BigDecimal.valueOf(400))
                .finalAmount(BigDecimal.valueOf(9600))
                .earnedPoints(BigDecimal.valueOf(96))
                .currency("KRW")
                .orderStatus(OrderStatus.PENDING_PAYMENT)
                .build();
        ReflectionTestUtils.setField(testOrder, "id", 1L);

        testUserPointBalance = UserPointBalance.builder()
                .user(testUser)
                .currentPoints(BigDecimal.valueOf(500))
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
        PointTransaction expired = new PointTransaction(testUser, null, BigDecimal.valueOf(100).negate(), PointType.EXPIRED);

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
        given(pointRepository.findPointTransactions(eq(1L), any(Pageable.class))).willReturn(new PageImpl<>(List.of(spent, refunded, earned, expired)));

        // When
        PageResponse<PointGetResponse> result = pointService.getPointHistory("test@test.com", PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(4);
        assertThat(result.getContent().get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(400).negate());
        assertThat(result.getContent().get(1).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(400));
        assertThat(result.getContent().get(2).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(result.getContent().get(3).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100).negate());
        assertThat(result.getContent().get(3).getOrderId()).isNull();
    }

    @Test
    @DisplayName("포인트 내역 조회 - 실패 (사용자 없음)")
    void getPointHistory_실패() {
        // Given
        given(userRepository.findByEmail("fail@test.com")).willReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class,
                () -> pointService.getPointHistory("fail@test.com", PageRequest.of(0, 10)));
    }


    // 포인트 잔액 조회 테스트
    @Test
    @DisplayName("포인트 잔액 조회 - 잔액 O")
    void checkPointBalance_잔액O() {
        // Given
        given(pointRepository.calculatePointBalance(1L))
                .willReturn(BigDecimal.valueOf(100));

        // When
        BigDecimal result = pointService.checkPointBalance(testUser);

        // Then
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("포인트 잔액 조회 - 잔액 X")
    void checkPointBalance_잔액X() {
        // Given
        given(pointRepository.calculatePointBalance(1L))
                .willReturn(null);

        // When
        BigDecimal result = pointService.checkPointBalance(testUser);

        // Then
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }


    // 포인트 사용 테스트
    @Test
    @DisplayName("포인트 사용")
    void usePoints_성공() {
        // Given
        PointTransaction earned1 = new PointTransaction(testUser, testOrder, BigDecimal.valueOf(200), PointType.EARNED);
        PointTransaction earned2 = new PointTransaction(testUser, testOrder, BigDecimal.valueOf(300), PointType.EARNED);

        given(pointRepository.findAvailablePoints(1L)).willReturn(List.of(earned1, earned2));
        given(userPointBalanceRepository.findByUserId(1L)).willReturn(Optional.of(testUserPointBalance));

        // When
        pointService.usePoints(testUser, testOrder);

        // Then
        assertThat(earned1.getRemainingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(earned2.getRemainingAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(testUserPointBalance.getCurrentPoints()).isEqualByComparingTo(BigDecimal.valueOf(100));

        verify(pointRepository, times(1)).save(argThat(transaction ->
                transaction.getType() == PointType.SPENT && transaction.getAmount().compareTo(BigDecimal.valueOf(400).negate()) == 0));
        verify(pointUsageRepository, times(2)).save(any(PointUsage.class));
    }


    // 포인트 복구 테스트
    @Test
    @DisplayName("포인트 복구")
    void refundPoints_성공() {
        // Given
        PointTransaction earned1 = new PointTransaction(testUser, testOrder, BigDecimal.valueOf(200), PointType.EARNED);
        PointTransaction earned2 = new PointTransaction(testUser, testOrder, BigDecimal.valueOf(300), PointType.EARNED);

        earned1.deduct(BigDecimal.valueOf(200));
        earned2.deduct(BigDecimal.valueOf(200));

        PointUsage usage1 = new PointUsage(earned1, testOrder, BigDecimal.valueOf(200));
        PointUsage usage2 = new PointUsage(earned2, testOrder, BigDecimal.valueOf(200));

        given(pointUsageRepository.findByOrderId(1L)).willReturn(List.of(usage1, usage2));
        given(userPointBalanceRepository.findByUserId(1L)).willReturn(Optional.of(testUserPointBalance));

        // When
        pointService.refundPoints(testUser, testOrder);

        // Then
        assertThat(earned1.getRemainingAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(earned2.getRemainingAmount()).isEqualByComparingTo(BigDecimal.valueOf(300));
        assertThat(testUserPointBalance.getCurrentPoints()).isEqualByComparingTo(BigDecimal.valueOf(900));

        verify(pointRepository, times(1)).save(argThat(transaction ->
                transaction.getType() == PointType.REFUNDED && transaction.getAmount().compareTo(BigDecimal.valueOf(400)) == 0));
    }


    // 포인트 적립 테스트
    @Test
    @DisplayName("포인트 적립")
    void earnPoints_성공() {
        // Given
        given(userPointBalanceRepository.findByUserId(1L)).willReturn(Optional.of(testUserPointBalance));

        // When
        pointService.earnPoints(testUser, testOrder);

        // Then
        assertThat(testUserPointBalance.getCurrentPoints()).isEqualByComparingTo(BigDecimal.valueOf(596));

        verify(pointRepository, times(1)).save(argThat(transaction ->
                transaction.getType() == PointType.EARNED && transaction.getAmount().compareTo(BigDecimal.valueOf(96)) == 0));
    }


    // 포인트 소멸 테스트
    @Test
    @DisplayName("포인트 소멸")
    void expirePoints_성공() {
        // Given
        PointTransaction earned = new PointTransaction(testUser, testOrder, BigDecimal.valueOf(200), PointType.EARNED);
        ReflectionTestUtils.setField(earned, "expiresAt", LocalDate.now().minusDays(1));

        given(pointRepository.findExpiredPoints()).willReturn(List.of(earned));
        given(userPointBalanceRepository.findByUserId(1L)).willReturn(Optional.of(testUserPointBalance));

        // When
        pointService.expirePoints();

        // Then
        assertThat(testUserPointBalance.getCurrentPoints()).isEqualByComparingTo(BigDecimal.valueOf(300));
        assertThat(earned.getRemainingAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        verify(pointRepository, times(1)).save(argThat(transaction ->
                transaction.getType() == PointType.EXPIRED && transaction.getAmount().compareTo(BigDecimal.valueOf(200).negate()) == 0));
    }


    // 스냅샷 정합성 보정 테스트
    @Test
    @DisplayName("스냅샷 정합성 보정 테스트 - 일치")
    void syncPointBalance_일치() {
        // Given
        given(userPointBalanceRepository.findAll()).willReturn(List.of(testUserPointBalance));
        given(pointRepository.calculatePointBalance(1L)).willReturn(BigDecimal.valueOf(500));

        // When
        pointService.syncPointBalance();

        // Then
        assertThat(testUserPointBalance.getCurrentPoints()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    @DisplayName("스냅샷 정합성 보정 테스트 - 불일치")
    void syncPointBalance_불일치() {
        // Given
        given(userPointBalanceRepository.findAll()).willReturn(List.of(testUserPointBalance));
        given(pointRepository.calculatePointBalance(1L)).willReturn(BigDecimal.valueOf(300));

        // When
        pointService.syncPointBalance();

        // Then
        assertThat(testUserPointBalance.getCurrentPoints()).isEqualByComparingTo(BigDecimal.valueOf(300));
    }
}