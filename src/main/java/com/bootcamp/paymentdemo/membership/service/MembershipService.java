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


    private static final BigDecimal NORMAL_MAX = new BigDecimal("50000");
    private static final BigDecimal VIP_MAX = new BigDecimal("149999");
    private static final BigDecimal VVIP_MIN = new BigDecimal("150000");


    @Transactional(readOnly = true)
    public List<Membership> getAllGradePolices() {
        return membershipRepository.findAll();
    }


    public Membership determineGrade(BigDecimal totalPaidAmount) {
        if (totalPaidAmount.compareTo(VVIP_MIN) >= 0) {

            return membershipRepository.findByGradeName(MembershipGrade.VVIP).orElseThrow(
                    () -> new MembershipNotFoundException("VVIP 등급을 찾을 수 없습니다"));
        } else if (totalPaidAmount.compareTo(NORMAL_MAX) > 0 && totalPaidAmount.compareTo(VIP_MAX) <= 0) {

            return membershipRepository.findByGradeName(MembershipGrade.VIP).orElseThrow(
                    () -> new MembershipNotFoundException("VIP 등급을 찾을 수 없습니다"));
        } else {

            return membershipRepository.findByGradeName(MembershipGrade.NORMAL).orElseThrow(
                    () -> new MembershipNotFoundException("NORMAL 등급을 찾을 수 없습니다"));
        }
    }



    @Transactional
    public boolean updateUserGrade(Long userId, Long orderId, String reason) {

        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId)
        );

        UserPaidAmount userPaidAmount = userPaidAmountRepository.findByUserId(userId).orElseThrow(
                () -> new UserPaidAmountNotFoundException("총 결제 금액 정보를 찾을 수 없습니다")
        );

        BigDecimal totalPaidAmount = userPaidAmount.getTotalPaidAmount();


        Membership newGrade = determineGrade(totalPaidAmount);


        Membership currentGrade = user.getCurrentGrade();

        if (currentGrade.getGradeName() == newGrade.getGradeName()) {
            log.debug("등급 변경 없음: userId={}, grade={}, amount={}",
                    userId, currentGrade.getGradeName(), totalPaidAmount);
            return false;
        }


        boolean upgraded = newGrade.getMinAmount().compareTo(currentGrade.getMinAmount()) > 0;

        log.info("등급 {} 감지: userId={}, {} → {}, amount={}",
                upgraded ? "상승" : "하락",
                userId,
                currentGrade.getGradeName(),
                newGrade.getGradeName(),
                totalPaidAmount);


        user.updateGrade(newGrade);
        userRepository.save(user);


        UserGradeHistory history = UserGradeHistory.createChange(
                user,
                currentGrade,
                newGrade,
                orderId,
                reason
        );
        userGradeHistoryRepository.save(history);

        return true;
    }



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


        boolean gradeChanged = updateUserGrade(userId, orderId, "주문 확정");

        if (gradeChanged) {
            log.info("결제 완료 후 등급 상승: userId={}, paymentId={}", userId, orderId);
        }
    }



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


        boolean gradeChanged = updateUserGrade(userId, orderId, "환불");

        if (gradeChanged) {
            log.warn("환불 후 등급 하락: userId={}, paymentId={}", userId, orderId);
        }
    }

}
