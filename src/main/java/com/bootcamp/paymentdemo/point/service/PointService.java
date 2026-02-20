package com.bootcamp.paymentdemo.point.service;

import com.bootcamp.paymentdemo.common.dto.PageResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;
    private final PointUsageRepository pointUsageRepository;
    private final UserRepository userRepository;
    private final UserPointBalanceRepository userPointBalanceRepository;


    @Transactional(readOnly = true)
    public PageResponse<PointGetResponse> getPointHistory(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () ->new UserNotFoundException("사용자를 찾을 수 없습니다.")
        );
        Page<PointTransaction> pointTransactionList = pointRepository.findPointTransactions(user.getUserId(), pageable);
        Page<PointGetResponse> page = pointTransactionList.map(PointGetResponse::from);

        return new PageResponse<>(page);
    }


    @Transactional(readOnly = true)
    public BigDecimal checkPointBalance(User user) {
        BigDecimal balance = pointRepository.calculatePointBalance(user.getUserId());

        log.info("포인트 잔액 조회: userId={}, 잔액={}", user.getUserId(), balance);

        return balance != null ? balance : BigDecimal.ZERO;
    }


    @Transactional
    public void usePoints(User user, Order order) {
        BigDecimal usedPoints = order.getUsedPoints();



        List<PointTransaction> earnedTransactionList = pointRepository.findAvailablePoints(user.getUserId());


        BigDecimal remaining = usedPoints;


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


        BigDecimal actualUsedPoints = usedPoints.subtract(remaining);


        if (actualUsedPoints.compareTo(BigDecimal.ZERO) > 0) {

            PointTransaction spentTransaction = new PointTransaction(
                    user, order, actualUsedPoints.negate(), PointType.SPENT);
            pointRepository.save(spentTransaction);


            updatePointBalance(user, actualUsedPoints.negate());

            log.info("포인트 사용 완료: userId={}, orderId={}, 요청 포인트={}, 실제 사용={}",
                user.getUserId(), order.getId(), usedPoints, actualUsedPoints);
        } else {
            log.warn("사용 가능한 포인트 없음: userId={}, orderId={}, 요청 포인트={}",
                user.getUserId(), order.getId(), usedPoints);
        }
    }


    @Transactional
    public void refundPoints(User user, Order order) {

        List<PointUsage> pointUsageList = pointUsageRepository.findByOrderId(order.getId());


        for (PointUsage pointUsage : pointUsageList) {
            pointUsage.getPointTransaction().restore(pointUsage.getAmount());
        }


        PointTransaction refundedTransaction = new PointTransaction(
                user, order, order.getUsedPoints(), PointType.REFUNDED);
        pointRepository.save(refundedTransaction);


        updatePointBalance(user, order.getUsedPoints());

        log.info("포인트 환불 완료: userId={}, orderId={}, 환불 포인트={}", user.getUserId(), order.getId(), order.getUsedPoints());
    }


    @Transactional
    public void earnPoints(User user, Order order) {

        BigDecimal pointsToEarn = order.getEarnedPoints();


        PointTransaction earnedTransaction = new PointTransaction(
                user, order, pointsToEarn, PointType.EARNED);
        pointRepository.save(earnedTransaction);


        updatePointBalance(user, pointsToEarn);

        log.info("포인트 적립 완료: userId={}, orderId={}, 적립 포인트={}", user.getUserId(), order.getId(), pointsToEarn);
    }


    @Transactional
    public int expirePoints() {

        List<PointTransaction> earnedTransactionList = pointRepository.findExpiredPoints();

        int successCount = 0;


        for (PointTransaction earnedTransaction : earnedTransactionList) {
            try {
                BigDecimal remaining = earnedTransaction.getRemainingAmount();
                PointTransaction expiredTransaction = new PointTransaction(
                        earnedTransaction.getUser(), null, remaining.negate(), PointType.EXPIRED);
                pointRepository.save(expiredTransaction);

                earnedTransaction.deduct(remaining);


                updatePointBalance(earnedTransaction.getUser(), remaining.negate());
                successCount++;

                log.info("포인트 소멸 완료: userId={}, 소멸 포인트={}", expiredTransaction.getUser().getUserId(), remaining);
            } catch (Exception e) {
                log.error("포인트 소멸 실패: pointId={}", earnedTransaction.getId());
            }
        }
        return successCount;
    }


    @Transactional
    public int syncPointBalance() {

        List<UserPointBalance> userPointBalanceList = userPointBalanceRepository.findAll();

        int successCount = 0;


        for (UserPointBalance userPointBalance : userPointBalanceList) {
            try {
                BigDecimal balance = pointRepository.calculatePointBalance(userPointBalance.getUserId());
                BigDecimal actualPointBalance = balance != null ? balance : BigDecimal.ZERO;
                if (actualPointBalance.compareTo(userPointBalance.getCurrentPoints()) != 0) {
                    userPointBalance.syncPointBalance(actualPointBalance);
                    successCount++;

                    log.info("포인트 정합성 보정: userId={}, 실제 포인트 잔액={}", userPointBalance.getUserId(), actualPointBalance);
                }
            } catch (Exception e) {
                log.error("포인트 정합성 보정 실패: userId={}", userPointBalance.getUserId());
            }
        }
        return successCount;
    }


    private void updatePointBalance(User user, BigDecimal amount) {
        UserPointBalance userPointBalance = userPointBalanceRepository.findByUserId(user.getUserId()).get();
        userPointBalance.updatePointBalance(amount);
    }
}
