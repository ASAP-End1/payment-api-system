package com.bootcamp.paymentdemo.point.service;

import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.point.dto.PointGetResponse;
import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import com.bootcamp.paymentdemo.point.entity.PointType;
import com.bootcamp.paymentdemo.point.entity.PointUsage;
import com.bootcamp.paymentdemo.point.exception.EarnedPointNotFoundException;
import com.bootcamp.paymentdemo.point.repository.PointRepository;
import com.bootcamp.paymentdemo.point.repository.PointUsageRepository;
import com.bootcamp.paymentdemo.user.entity.User;
import com.bootcamp.paymentdemo.user.entity.UserPointBalance;
import com.bootcamp.paymentdemo.user.exception.UserNotFoundException;
import com.bootcamp.paymentdemo.user.repository.UserPointBalanceRepository;
import com.bootcamp.paymentdemo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;
    private final PointUsageRepository pointUsageRepository;
    private final UserRepository userRepository;
    private final UserPointBalanceRepository userPointBalanceRepository;

    // TODO 페이징 적용
    // 포인트 내역 조회
    @Transactional(readOnly = true)
    public List<PointGetResponse> getPointHistory(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () ->new UserNotFoundException("사용자가 존재하지 않습니다")
        );
        List<PointTransaction> pointTransactionList = pointRepository.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId());
        return pointTransactionList.stream()
                .map(pointTransaction -> new PointGetResponse(
                        pointTransaction.getId(),
                        // EXPIRED 타입은 Order가 null
                        pointTransaction.getOrder() != null ? pointTransaction.getOrder().getId() : null,
                        pointTransaction.getAmount(),
                        pointTransaction.getType(),
                        pointTransaction.getCreatedAt(),
                        pointTransaction.getExpiresAt()
                )).toList();
    }

    // 포인트 잔액 조회
    @Transactional(readOnly = true)
    public BigDecimal checkPointBalance(User user) {
        BigDecimal balance = pointRepository.calculatePointBalance(user.getUserId());

        log.info("포인트 잔액 조회: userId={}, 잔액={}", user.getUserId(), balance);

        return balance != null ? balance : BigDecimal.ZERO;
    }

    // 포인트 사용
    @Transactional
    public void usePoints(User user, Order order) {
        BigDecimal usedPoints = order.getUsedPoints();

        // 사용 가능한 적립 포인트 조회
        // 만료일 임박한 포인트부터 사용
        List<PointTransaction> earnedTransactionList = pointRepository
                .findByUser_UserIdAndTypeAndRemainingAmountGreaterThanAndExpiresAtAfterOrderByExpiresAtAsc(
                        user.getUserId(), PointType.EARNED, BigDecimal.ZERO, LocalDate.now());

        // 차감하고 남은 포인트
        BigDecimal remaining = usedPoints;

        // 조회한 포인트 PointUsage에 저장, 잔액 차감
        for (PointTransaction earnedTransaction : earnedTransactionList) {
            BigDecimal deductAmount;
            if (remaining.compareTo(earnedTransaction.getRemainingAmount()) >= 0) {
                deductAmount = earnedTransaction.getRemainingAmount();
            } else {
                deductAmount = remaining;
            }

            PointUsage pointUsage = new PointUsage(earnedTransaction, order, deductAmount);
            pointUsageRepository.save(pointUsage);
            earnedTransaction.deduct(deductAmount);

            remaining = remaining.subtract(deductAmount);
            if (remaining.compareTo(BigDecimal.ZERO) == 0) break;
        }

        // 사용 포인트 내역 PointTransaction에 저장
        PointTransaction spentTransaction = new PointTransaction(
                user, order, usedPoints.negate(), PointType.SPENT);
        pointRepository.save(spentTransaction);

        // 스냅샷 업데이트
        updatePointBalance(user, usedPoints.negate());

        log.info("포인트 사용 완료: userId={}, orderId={}, 사용 포인트={}", user.getUserId(), order.getId(), usedPoints);
    }

    // 포인트 복구
    @Transactional
    public void refundPoints(User user, Order order) {
        // 주문 id로 사용한 포인트 내역 조회
        List<PointUsage> pointUsageList = pointUsageRepository.findByOrderId(order.getId());

        // remainingAmount 복구
        for (PointUsage pointUsage : pointUsageList) {
            pointUsage.getPointTransaction().restore(pointUsage.getAmount());
        }

        // 환불 포인트 내역 PointTransaction에 저장
        PointTransaction refundedTransaction = new PointTransaction(
                user, order, order.getUsedPoints(), PointType.REFUNDED);
        pointRepository.save(refundedTransaction);

        // 스냅샷 업데이트
        updatePointBalance(user, order.getUsedPoints());

        log.info("포인트 환불 완료: userId={}, orderId={}, 환불 포인트={}", user.getUserId(), order.getId(), order.getUsedPoints());
    }

    // 포인트 적립
    @Transactional
    public void earnPoints(User user, Order order) {
        // 사용자의 멤버십 등급에 따라 적립할 포인트 계산
        BigDecimal pointsToEarn = order.getFinalAmount().multiply(user.getCurrentGrade().getAccRate())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        // 적립 포인트 내역 PointTransaction에 저장
        PointTransaction earnedTransaction = new PointTransaction(
                user, order, pointsToEarn, PointType.EARNED);
        pointRepository.save(earnedTransaction);

        // 스냅샷 업데이트
        updatePointBalance(user, pointsToEarn);

        log.info("포인트 적립 완료: userId={}, orderId={}, 적립 포인트={}", user.getUserId(), order.getId(), pointsToEarn);
    }

    // 포인트 적립 취소
    @Transactional
    public void cancelEarnedPoints(User user, Order order) {
        // 해당 주문에서 적립된 포인트 조회
        PointTransaction earnedTransaction = pointRepository.findByOrderIdAndType(order.getId(), PointType.EARNED).orElseThrow(
                () -> new EarnedPointNotFoundException("적립금이 존재하지 않습니다.")
        );
        BigDecimal earnedPoints = earnedTransaction.getAmount();

        // remainingAmount 0으로 변경
        earnedTransaction.deduct(earnedTransaction.getRemainingAmount());

        // 적립 취소 내역 PointTransaction에 저장
        PointTransaction canceledTransaction = new PointTransaction(
                user, order, earnedPoints.negate(), PointType.CANCELED);
        pointRepository.save(canceledTransaction);

        // 스냅샷 업데이트
        updatePointBalance(user, earnedPoints.negate());

        log.info("포인트 적립 취소 완료: userId={}, orderId={}, 취소 포인트={}", user.getUserId(), order.getId(), earnedPoints);
    }

    // 포인트 소멸 (매일 00시 실행)
    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void expirePoints() {
        // remainingAmount가 0보다 크고, 만료일이 지난 포인트 조회
        List<PointTransaction> earnedTransactionList = pointRepository
                .findByTypeAndRemainingAmountGreaterThanAndExpiresAtBefore(PointType.EARNED, BigDecimal.ZERO, LocalDate.now());

        // PointTransaction에 저장, remainingAmount 0으로 변경
        for (PointTransaction earnedTransaction : earnedTransactionList) {
            BigDecimal remaining = earnedTransaction.getRemainingAmount();
            PointTransaction expiredTransaction = new PointTransaction(
                    earnedTransaction.getUser(), null, remaining.negate(), PointType.EXPIRED);
            pointRepository.save(expiredTransaction);

            earnedTransaction.deduct(remaining);

            // 스냅샷 업데이트
            updatePointBalance(earnedTransaction.getUser(), remaining.negate());

            log.info("포인트 소멸 완료: userId={}, 소멸 포인트={}", expiredTransaction.getUser().getUserId(), remaining);
        }
    }

    // 스냅샷 정합성 보정 (매일 00시 30분 실행 - 소멸 후)
    @Transactional
    @Scheduled(cron = "0 30 0 * * *")
    public void syncPointBalance() {
        // UserPointBalance 리스트 조회
        List<UserPointBalance> userPointBalanceList = userPointBalanceRepository.findAll();

        // UserPointBalance의 currentPoints와 실제 포인트가 다르면 보정
        for (UserPointBalance userPointBalance : userPointBalanceList) {
            BigDecimal balance = pointRepository.calculatePointBalance(userPointBalance.getUserId());
            BigDecimal actualPointBalance = balance != null ? balance : BigDecimal.ZERO;
            if (actualPointBalance.compareTo(userPointBalance.getCurrentPoints()) != 0) {
                userPointBalance.syncPointBalance(actualPointBalance);

                log.info("포인트 정합성 보정: userId={}, 실제 포인트 잔액={}", userPointBalance.getUserId(), actualPointBalance);
            }
        }
    }

    // 스냅샷 업데이트
    private void updatePointBalance(User user, BigDecimal amount) {
        UserPointBalance userPointBalance = userPointBalanceRepository.findByUserId(user.getUserId()).get();
        userPointBalance.updatePointBalance(amount);
    }
}
