package com.bootcamp.paymentdemo.point.service;

import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.point.dto.PointGetResponse;
import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import com.bootcamp.paymentdemo.point.entity.PointType;
import com.bootcamp.paymentdemo.point.entity.PointUsage;
import com.bootcamp.paymentdemo.point.repository.PointRepository;
import com.bootcamp.paymentdemo.point.repository.PointUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;
    private final PointUsageRepository pointUsageRepository;

    // TODO 페이징 적용
    // 포인트 내역 조회
    @Transactional(readOnly = true)
    public List<PointGetResponse> getPointHistory(Long userId) {
        List<PointTransaction> pointTransactions = pointRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return pointTransactions.stream()
                .map(pointTransaction -> new PointGetResponse(
                        pointTransaction.getId(),
                        pointTransaction.getOrder().getId(),
                        pointTransaction.getAmount(),
                        pointTransaction.getType(),
                        pointTransaction.getCreatedAt(),
                        pointTransaction.getExpiresAt()
                )).toList();
    }

    // 포인트 잔액 조회
    @Transactional(readOnly = true)
    public int checkPointBalance(Long userId) {
        Long balance = pointRepository.calculateBalance(userId);
        return balance != null ? balance.intValue() : 0;
    }

    // TODO User 엔티티 연결 -> usePoints(User user, Order order)로 변경
    // 포인트 사용
    @Transactional
    public void usePoints(Long userId, Order order) {
        int usedPoints = order.getUsedPoints();

        // 사용 가능한 적립 포인트 조회
        // 만료일 임박한 포인트부터 사용
        List<PointTransaction> earnedPointsList = pointRepository
                .findByUserIdAndTypeAndRemainingAmountGreaterThanAndExpiresAtAfterOrderByExpiresAtAsc(
                        userId, PointType.EARNED, 0, LocalDate.now());

        // 차감하고 남은 포인트
        int remaining = usedPoints;

        // 조회한 포인트 PointUsage에 저장, 잔액 차감
        for (PointTransaction earnedPoints : earnedPointsList) {
            int deductAmount;
            if (remaining >= earnedPoints.getRemainingAmount()) {
                deductAmount = earnedPoints.getRemainingAmount();
            } else {
                deductAmount = remaining;
            }

            PointUsage pointUsage = new PointUsage(earnedPoints, order, deductAmount);
            pointUsageRepository.save(pointUsage);
            earnedPoints.deduct(deductAmount);

            remaining -= deductAmount;
            if (remaining == 0 ) break;
        }

        // 사용 포인트 내역 PointTransaction에 저장
        PointTransaction pointTransaction = new PointTransaction(
                userId, order, -usedPoints, PointType.SPENT);
        pointRepository.save(pointTransaction);
//        updateBalance(userId);
    }

    // 포인트 복구
    @Transactional
    public void refundPoints(Long userId, Order order) {
        // 주문 id로 사용한 포인트 내역 조회
        List<PointUsage> pointUsageList = pointUsageRepository.findByOrderId(order.getId());

        // remainingAmount 복구
        for (PointUsage pointUsage : pointUsageList) {
            pointUsage.getPointTransaction().restore(pointUsage.getAmount());
        }

        // 환불 포인트 내역 PointTransaction에 저장
        PointTransaction pointTransaction = new PointTransaction(
                userId, order, order.getUsedPoints(), PointType.REFUNDED);
        pointRepository.save(pointTransaction);
    }

    // TODO User 엔티티 연결 -> earnPoints(User user, Order order)로 변경
    // 포인트 적립
    @Transactional
    public void earnPoints(Long userId, Order order, int pointsToEarn) {
//        int pointsToEarn = order.getFinalAmount() * user.getCurrentGradeId().getAccRate() / 100;
        PointTransaction pointTransaction = new PointTransaction(
                userId, order, pointsToEarn, PointType.EARNED);
        pointRepository.save(pointTransaction);
//        updateBalance(userId);
    }

    // TODO User 엔티티 연결 -> cancelEarnedPoints(User user, Order order)로 변경
    // 포인트 적립 취소
    @Transactional
    public void cancelEarnedPoints(Long userId, Order order) {
        PointTransaction earnedPointTransaction = pointRepository.findByOrderIdAndType(order.getId(), PointType.EARNED).orElseThrow(
                () -> new IllegalArgumentException("적립금이 존재하지 않습니다.")
        );
        int earnedPoints = earnedPointTransaction.getAmount();
        PointTransaction pointTransaction = new PointTransaction(
                userId, order, -earnedPoints, PointType.CANCELED);
        pointRepository.save(pointTransaction);
//        updateBalance(userId);
    }

    // 스냅샷 업데이트
    // TODO UserPointBalance 한비님이 구현
//    private void updateBalance(Long userId) {
//        int balance = checkPointBalance(userId);
//        UserPointBalance userPointBalance = userPointBalanceRepository.findByUserId(userId);
//        userPointBalance.updateBlance(balance);
//    }
}
