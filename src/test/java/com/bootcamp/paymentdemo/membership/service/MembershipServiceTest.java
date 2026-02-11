package com.bootcamp.paymentdemo.membership.service;

import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.membership.entity.MembershipGrade;
import com.bootcamp.paymentdemo.membership.repository.MembershipRepository;
import com.bootcamp.paymentdemo.user.entity.User;
import com.bootcamp.paymentdemo.user.entity.UserGradeHistory;
import com.bootcamp.paymentdemo.user.entity.UserPaidAmount;
import com.bootcamp.paymentdemo.user.repository.UserGradeHistoryRepository;
import com.bootcamp.paymentdemo.user.repository.UserPaidAmountRepository;
import com.bootcamp.paymentdemo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Transactional
class MembershipServiceTest {

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPaidAmountRepository userPaidAmountRepository;

    @Autowired
    private UserGradeHistoryRepository userGradeHistoryRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    private User testUser;
    private Membership normalGrade;

    @BeforeEach
    void setUp() {
        normalGrade = membershipRepository.findByGradeName(MembershipGrade.NORMAL)
                .orElseThrow(() -> new IllegalStateException("NORMAL 등급이 없습니다"));

        // 테스트 사용자 생성
        testUser = User.register(
                "test@example.com",
                "encodedPassword",
                "테스트유저",
                "010-1234-5678",
                normalGrade
        );
        testUser = userRepository.save(testUser);

        // 총 결제 금액 초기화 (0원)
        UserPaidAmount paidAmount = UserPaidAmount.createDefault(testUser);
        userPaidAmountRepository.save(paidAmount);

        // 초기 등급 이력 생성
        UserGradeHistory initialHistory = UserGradeHistory.createInitial(testUser, normalGrade);
        userGradeHistoryRepository.save(initialHistory);
    }

    @Test
    @DisplayName("경계값 테스트 - 50,000원 이하는 NOMAL 등급")
    void handlePaymentCompleted_BoundaryTest_NormalMax() {
        // given

        // when [50,000원 결제]
        membershipService.handlePaymentCompleted(testUser.getUserId(), new BigDecimal("50000"), 1L);

        // then
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(updatedUser.getCurrentGrade().getGradeName()).isEqualTo(MembershipGrade.NORMAL);
    }

    @Test
    @DisplayName("경계값 테스트 - 50,000원 초과는 VIP 등급")
    void handlePaymentCompleted_BoundaryTest_VipMin() {
        // given

        // when [50,001원 결제]
        membershipService.handlePaymentCompleted(testUser.getUserId(), new BigDecimal("50001"), 1L);

        // then
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(updatedUser.getCurrentGrade().getGradeName()).isEqualTo(MembershipGrade.VIP);
    }

    @Test
    @DisplayName("경계값 테스트 - 150,000원 미만은 VIP 등급")
    void handlePaymentCompleted_BoundaryTest_VipMax() {
        // given

        // when [149,999원 결제]
        membershipService.handlePaymentCompleted(testUser.getUserId(), new BigDecimal("149999"), 1L);

        // then
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(updatedUser.getCurrentGrade().getGradeName()).isEqualTo(MembershipGrade.VIP);
    }

    @Test
    @DisplayName("경계값 테스트 - 150,000원 이상은 VVIP 등급)")
    void handlePaymentCompleted_BoundaryTest_VvipMin() {
        // given

        // when [150,000원 결제]
        membershipService.handlePaymentCompleted(testUser.getUserId(), new BigDecimal("150000"), 1L);

        // then
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(updatedUser.getCurrentGrade().getGradeName()).isEqualTo(MembershipGrade.VVIP);
    }

    @Test
    @DisplayName("결제 완료 - 총 결제 금액 증가 및 NORMAL → VIP 등급 상승")
    void handlePaymentCompleted_UpgradeToVIP() {
        // given
        UserPaidAmount initialPaidAmount = userPaidAmountRepository.findByUserId(testUser.getUserId())
                .orElseThrow();

        assertThat(initialPaidAmount.getTotalPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(testUser.getCurrentGrade().getGradeName()).isEqualTo(MembershipGrade.NORMAL);

        // when [60,000원 결제]
        BigDecimal paymentAmount = new BigDecimal("60000");
        Long paymentId = 1L;

        membershipService.handlePaymentCompleted(testUser.getUserId(), paymentAmount, paymentId);

        // then [총 결제 금액 증가 확인]
        UserPaidAmount updatedPaidAmount = userPaidAmountRepository.findByUserId(testUser.getUserId())
                .orElseThrow();

        assertThat(updatedPaidAmount.getTotalPaidAmount())
                .isEqualByComparingTo(new BigDecimal("60000"));

        // 등급 상승 확인
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(updatedUser.getCurrentGrade().getGradeName()).isEqualTo(MembershipGrade.VIP);

        // 해당 사용자의 등급 변경 이력 확인
        List<UserGradeHistory> histories = userGradeHistoryRepository
                .findByUserUserIdOrderByUpdatedAtAsc(testUser.getUserId());
        assertThat(histories).hasSize(2);

        UserGradeHistory upgradeHistory = histories.get(1);
        assertThat(upgradeHistory.getFromGrade().getGradeName()).isEqualTo(MembershipGrade.NORMAL);
        assertThat(upgradeHistory.getToGrade().getGradeName()).isEqualTo(MembershipGrade.VIP);
        assertThat(upgradeHistory.getTriggerPaymentId()).isEqualTo(paymentId);
        assertThat(upgradeHistory.getReason()).isEqualTo("결제 완료");
    }

    @Test
    @DisplayName("결제 완료 - 총 결제 금액 증가 및 NORMAL → VVIP 등급 상승")
    void handlePaymentCompleted_UpgradeToVVIP() {
        // given

        // when [160,000원 결제]
        BigDecimal paymentAmount = new BigDecimal("160000");
        Long paymentId = 2L;

        membershipService.handlePaymentCompleted(testUser.getUserId(), paymentAmount, paymentId);

        // then [총 결제 금액 확인]
        UserPaidAmount updatedPaidAmount = userPaidAmountRepository.findByUserId(testUser.getUserId())
                .orElseThrow();

        assertThat(updatedPaidAmount.getTotalPaidAmount())
                .isEqualByComparingTo(new BigDecimal("160000"));

        // 등급 상승 확인
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(updatedUser.getCurrentGrade().getGradeName()).isEqualTo(MembershipGrade.VVIP);
    }

    @Test
    @DisplayName("결제 완료 - 등급 변경 없음 (NORMAL 유지)")
    void handlePaymentCompleted_StayNormal() {
        // given

        // when [30,000원 결제]
        BigDecimal paymentAmount = new BigDecimal("30000");
        Long paymentId = 3L;

        membershipService.handlePaymentCompleted(testUser.getUserId(), paymentAmount, paymentId);

        // then [총 결제 금액 확인]
        UserPaidAmount updatedPaidAmount = userPaidAmountRepository.findByUserId(testUser.getUserId())
                .orElseThrow();

        assertThat(updatedPaidAmount.getTotalPaidAmount())
                .isEqualByComparingTo(new BigDecimal("30000"));

        // 등급 확인 (NORMAL 유지)
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(updatedUser.getCurrentGrade().getGradeName()).isEqualTo(MembershipGrade.NORMAL);

        // 해당 사용자의 등급 변경 이력이 추가되지 않음
        List<UserGradeHistory> histories = userGradeHistoryRepository
                .findByUserUserIdOrderByUpdatedAtAsc(testUser.getUserId());
        assertThat(histories).hasSize(1); // 초기 이력만 존재
    }

    @Test
    @DisplayName("환불 완료 - 총 결제 금액 감소 및 VVIP → VIP 등급 하락")
    void handleRefund_DowngradeFromVVIPToVIP() {
        // given [VVIP 등급으로 설정 (160,000원 결제)]
        BigDecimal initialPayment = new BigDecimal("160000");
        membershipService.handlePaymentCompleted(testUser.getUserId(), initialPayment, 1L);

        User userAfterPayment = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(userAfterPayment.getCurrentGrade().getGradeName()).isEqualTo(MembershipGrade.VVIP);

        // when [70,000원 환불]
        BigDecimal refundAmount = new BigDecimal("70000");
        Long refundPaymentId = 2L;

        membershipService.handleRefund(testUser.getUserId(), refundAmount, refundPaymentId);

        // then [총 결제 금액 감소 확인]
        UserPaidAmount updatedPaidAmount = userPaidAmountRepository.findByUserId(testUser.getUserId())
                .orElseThrow();

        assertThat(updatedPaidAmount.getTotalPaidAmount())
                .isEqualByComparingTo(new BigDecimal("90000"));

        // 등급 하락 확인
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(updatedUser.getCurrentGrade().getGradeName()).isEqualTo(MembershipGrade.VIP);

        // 해당 사용자의 등급 변경 이력 확인
        List<UserGradeHistory> histories = userGradeHistoryRepository
                .findByUserUserIdOrderByUpdatedAtAsc(testUser.getUserId());
        assertThat(histories).hasSize(3); // 초기 이력 + 상승(VVIP)이력 + 하락(VIP) 이력

        UserGradeHistory downgradeHistory = histories.get(2);
        assertThat(downgradeHistory.getFromGrade().getGradeName()).isEqualTo(MembershipGrade.VVIP);
        assertThat(downgradeHistory.getToGrade().getGradeName()).isEqualTo(MembershipGrade.VIP);
        assertThat(downgradeHistory.getTriggerPaymentId()).isEqualTo(refundPaymentId);
        assertThat(downgradeHistory.getReason()).isEqualTo("환불");
    }

    @Test
    @DisplayName("환불 완료 - 총 결제 금액 감소 및 VIP → NORMAL 등급 하락")
    void handleRefund_DowngradeFromVIPToNormal() {
        // given [VIP 등급으로 설정 (60,000원 결제)]
        BigDecimal initialPayment = new BigDecimal("60000");
        membershipService.handlePaymentCompleted(testUser.getUserId(), initialPayment, 1L);

        User userAfterPayment = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(userAfterPayment.getCurrentGrade().getGradeName()).isEqualTo(MembershipGrade.VIP);

        // when [20,000원 환불]
        BigDecimal refundAmount = new BigDecimal("20000");
        Long refundPaymentId = 2L;

        membershipService.handleRefund(testUser.getUserId(), refundAmount, refundPaymentId);

        // then [총 결제 금액 감소 확인]
        UserPaidAmount updatedPaidAmount = userPaidAmountRepository.findByUserId(testUser.getUserId())
                .orElseThrow();

        assertThat(updatedPaidAmount.getTotalPaidAmount())
                .isEqualByComparingTo(new BigDecimal("40000"));

        // 등급 하락 확인
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(updatedUser.getCurrentGrade().getGradeName()).isEqualTo(MembershipGrade.NORMAL);

        // 해당 사용자의 등급 변경 이력 확인
        List<UserGradeHistory> histories = userGradeHistoryRepository
                .findByUserUserIdOrderByUpdatedAtAsc(testUser.getUserId());
        assertThat(histories).hasSize(3); // 초기 이력 + 상승(VIP) 이력 + 하락(NORMAL) 이력

        UserGradeHistory downgradeHistory = histories.get(2);
        assertThat(downgradeHistory.getFromGrade().getGradeName()).isEqualTo(MembershipGrade.VIP);
        assertThat(downgradeHistory.getToGrade().getGradeName()).isEqualTo(MembershipGrade.NORMAL);
    }

    @Test
    @DisplayName("환불 완료 - 등급 변경 없음 (VIP 유지)")
    void handleRefund_ShouldStayVIP() {
        // given [VIP 등급으로 설정 (80,000원 결제)]
        BigDecimal initialPayment = new BigDecimal("80000");
        membershipService.handlePaymentCompleted(testUser.getUserId(), initialPayment, 1L);

        User userAfterPayment = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(userAfterPayment.getCurrentGrade().getGradeName()).isEqualTo(MembershipGrade.VIP);

        // when [10,000원 환불]
        BigDecimal refundAmount = new BigDecimal("10000");
        Long refundPaymentId = 2L;

        membershipService.handleRefund(testUser.getUserId(), refundAmount, refundPaymentId);

        // then [총 결제 금액 감소 확인]
        UserPaidAmount updatedPaidAmount = userPaidAmountRepository.findByUserId(testUser.getUserId())
                .orElseThrow();

        assertThat(updatedPaidAmount.getTotalPaidAmount())
                .isEqualByComparingTo(new BigDecimal("70000"));

        // 등급 확인
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(updatedUser.getCurrentGrade().getGradeName()).isEqualTo(MembershipGrade.VIP);

        // 해당 사용자의 등급 변경 이력 확인
        List<UserGradeHistory> histories = userGradeHistoryRepository
                .findByUserUserIdOrderByUpdatedAtAsc(testUser.getUserId());
        assertThat(histories).hasSize(2); // 초기 이력 + 상승(VIP) 이력
    }

    @Test
    @DisplayName("여러 번 결제 및 환불 후 등급 변화")
    void handleMultiplePaymentsAndRefunds() {
        // given

        // when [1차 결제 60,000원]
        membershipService.handlePaymentCompleted(testUser.getUserId(), new BigDecimal("60000"), 1L);

        // then [VIP 등급 확인]
        User user1 = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(user1.getCurrentGrade().getGradeName()).isEqualTo(MembershipGrade.VIP);

        // when [2차 결제 100,000원 (총 결제 금액 160,000원)]
        membershipService.handlePaymentCompleted(testUser.getUserId(), new BigDecimal("100000"), 2L);

        // then [VVIP 등급 확인]
        User user2 = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(user2.getCurrentGrade().getGradeName()).isEqualTo(MembershipGrade.VVIP);

        // when [환불 80,000원 (총 결제 금액 80,000원)]
        membershipService.handleRefund(testUser.getUserId(), new BigDecimal("80000"), 3L);

        // then [VIP 등급 확인]
        User user3 = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(user3.getCurrentGrade().getGradeName()).isEqualTo(MembershipGrade.VIP);

        // then [총 결제 금액 확인]
        UserPaidAmount finalPaidAmount = userPaidAmountRepository.findByUserId(testUser.getUserId())
                .orElseThrow();
        assertThat(finalPaidAmount.getTotalPaidAmount())
                .isEqualByComparingTo(new BigDecimal("80000"));

        // then [해당 사용자의 등급 변경 이력 확인]
        List<UserGradeHistory> histories = userGradeHistoryRepository
                .findByUserUserIdOrderByUpdatedAtAsc(testUser.getUserId());
        assertThat(histories).hasSize(4); // 초기 + VIP + VVIP + VIP
    }

}