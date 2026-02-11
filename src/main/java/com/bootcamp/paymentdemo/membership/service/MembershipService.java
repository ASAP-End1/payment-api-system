package com.bootcamp.paymentdemo.membership.service;

import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.membership.entity.MembershipGrade;
import com.bootcamp.paymentdemo.membership.exception.MembershipNotFoundException;
import com.bootcamp.paymentdemo.membership.exception.UserPaidAmountNotFoundException;
import com.bootcamp.paymentdemo.membership.repository.MembershipRepository;
import com.bootcamp.paymentdemo.user.entity.User;
import com.bootcamp.paymentdemo.user.entity.UserGradeHistory;
import com.bootcamp.paymentdemo.user.entity.UserPaidAmount;
import com.bootcamp.paymentdemo.user.exception.UserNotFoundException;
import com.bootcamp.paymentdemo.user.repository.UserGradeHistoryRepository;
import com.bootcamp.paymentdemo.user.repository.UserPaidAmountRepository;
import com.bootcamp.paymentdemo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final UserPaidAmountRepository userPaidAmountRepository;
    private final UserGradeHistoryRepository userGradeHistoryRepository;

    // 등급 기준 금액
    private static final BigDecimal NORMAL_MAX = new BigDecimal("50000");   // 5만원 이하
    private static final BigDecimal VIP_MAX = new BigDecimal("149999");     // 10만원 이하 -> 상의 후 변경 필요
    private static final BigDecimal VVIP_MIN = new BigDecimal("150000");    // 15만원 이상

    // 멤버십 등급 정책 조회
    @Transactional(readOnly = true)
    public List<Membership> getAllGradePolices() {
        return membershipRepository.findAll();
    }

    // 총 결제 금액으로 등급 결정 (총 결제금액 -> 해당하는 등급 출력)
    public Membership determineGrade(BigDecimal totalPaidAmount) {
        if (totalPaidAmount.compareTo(VVIP_MIN) >= 0) {
            // 15만원 이상 - VVIP
            return membershipRepository.findByGradeName(MembershipGrade.VVIP).orElseThrow(
                    () -> new MembershipNotFoundException("VVIP 등급을 찾을 수 없습니다"));
        } else if (totalPaidAmount.compareTo(NORMAL_MAX) > 0 && totalPaidAmount.compareTo(VIP_MAX) <= 0) {
            // 5만원 초과 ~ 149999원 이하 - VIP
            return membershipRepository.findByGradeName(MembershipGrade.VIP).orElseThrow(
                    () -> new MembershipNotFoundException("VIP 등급을 찾을 수 없습니다"));
        } else {
            // 5만원 이하 - NORMAL
            return membershipRepository.findByGradeName(MembershipGrade.NORMAL).orElseThrow(
                    () -> new MembershipNotFoundException("NORMAL 등급을 찾을 수 없습니다"));
        }
    }

    // 사용자 등급 업데이트
    // userId, orderId(등급 변경을 발생시킨 주문 id), reason(등급 변경 사유)
    @Transactional
    public boolean updateUserGrade(Long userId, Long orderId, String reason) {

        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId)
        );

        UserPaidAmount userPaidAmount = userPaidAmountRepository.findByUserId(userId).orElseThrow(
                () -> new UserPaidAmountNotFoundException("총 결제 금액 정보를 찾을 수 없습니다")
        );

        BigDecimal totalPaidAmount = userPaidAmount.getTotalPaidAmount();

        // 새로운 등급 결정
        Membership newGrade = determineGrade(totalPaidAmount);

        // 현재 등급과 비교
        Membership currentGrade = user.getCurrentGrade();

        if (currentGrade.getGradeName() == newGrade.getGradeName()) {
            log.debug("등급 변경 없음: userId={}, grade={}, amount={}",
                    userId, currentGrade.getGradeName(), totalPaidAmount);
            return false; // 등급 변경 없음
        }

        // 등급 변경
        boolean upgraded = newGrade.getMinAmount().compareTo(currentGrade.getMinAmount()) > 0;

        log.info("등급 {} 감지: userId={}, {} → {}, amount={}",
                upgraded ? "상승" : "하락",
                userId,
                currentGrade.getGradeName(),
                newGrade.getGradeName(),
                totalPaidAmount);

        // User 엔티티 멤버십 등급 업데이트
        user.updateGrade(newGrade);
        userRepository.save(user);

        // 등급 변경 이력 저장
        UserGradeHistory history = UserGradeHistory.createChange(
                user,
                currentGrade,
                newGrade,
                orderId,
                reason
        );
        userGradeHistoryRepository.save(history);

        return true; // 등급 변경됨
    }

    // 주문 확정 시 총 결제 금액 증가 & 등급 업데이트
    // userId, orderAmount(포인트 차감 후 금액-finalAmount), orderId(등급 변경을 발생시킨 주문 id)
    @Transactional
    public void handleOrderCompleted(Long userId, BigDecimal orderAmount, Long orderId) {

        UserPaidAmount userPaidAmount = userPaidAmountRepository.findByUserId(userId).orElseThrow(
                () -> new UserPaidAmountNotFoundException("총 결제 금액 정보를 찾을 수 없습니다")
        );

        userPaidAmount.addPaidAmount(orderAmount);
        userPaidAmountRepository.save(userPaidAmount);

        log.info("총 결제 금액 증가: userId={}, 이전={}, 결제={}, 합계={}",
                userId,
                userPaidAmount.getTotalPaidAmount().subtract(orderAmount),
                orderAmount,
                userPaidAmount.getTotalPaidAmount());

        // 등급 업데이트
        boolean gradeChanged = updateUserGrade(userId, orderId, "주문 확정");

        if (gradeChanged) {
            log.info("결제 완료 후 등급 상승: userId={}, paymentId={}", userId, orderId);
        }
    }

    // 환불 완료 시 총 결제 금액 차감 & 등급 업데이트
    // userId, refundAmount(환불 금액), orderId(등급 변경을 발생시킨 주문 id)
    @Transactional
    public void handleRefund(Long userId, BigDecimal refundAmount, Long orderId) {

        UserPaidAmount userPaidAmount = userPaidAmountRepository.findByUserId(userId).orElseThrow(
                () -> new UserPaidAmountNotFoundException("총 결제 금액 정보를 찾을 수 없습니다")
        );

        userPaidAmount.subtractPaidAmount(refundAmount);
        userPaidAmountRepository.save(userPaidAmount);

        log.info("총 결제 금액 감소: userId={}, 이전={}, 환불={}, 합계={}",
                userId,
                userPaidAmount.getTotalPaidAmount().add(refundAmount),
                refundAmount,
                userPaidAmount.getTotalPaidAmount());

        // 등급 업데이트
        boolean gradeChanged = updateUserGrade(userId, orderId, "환불");

        if (gradeChanged) {
            log.warn("환불 후 등급 하락: userId={}, paymentId={}", userId, orderId);
        }
    }

}
