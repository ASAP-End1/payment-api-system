package com.bootcamp.paymentdemo.point.service;

import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.point.dto.PointGetResponse;
import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import com.bootcamp.paymentdemo.point.entity.PointType;
import com.bootcamp.paymentdemo.point.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;

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
        PointTransaction pointTransaction = new PointTransaction(
                userId, order, -usedPoints, PointType.SPENT);
        pointRepository.save(pointTransaction);
//        updateBalance(userId);
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
