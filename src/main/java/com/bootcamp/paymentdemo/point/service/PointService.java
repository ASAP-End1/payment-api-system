package com.bootcamp.paymentdemo.point.service;

import com.bootcamp.paymentdemo.point.dto.PointGetResponse;
import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import com.bootcamp.paymentdemo.point.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                        pointTransaction.getOrderId(),
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
}
