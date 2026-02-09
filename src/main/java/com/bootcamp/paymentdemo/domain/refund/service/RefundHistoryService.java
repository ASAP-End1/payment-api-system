package com.bootcamp.paymentdemo.domain.refund.service;

import com.bootcamp.paymentdemo.domain.refund.entity.Refund;
import com.bootcamp.paymentdemo.domain.refund.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundHistoryService {

    private final RefundRepository refundRepository;

    // 환불 요청 이력 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveRequestHistory(Payment payment, String reason) {
        Refund requestRefund = Refund.createRequest(
                payment, payment.getAmount(), reason
        );

        refundRepository.save(refund);
    }

    // 환불 실패 이력 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailHistory(Payment payment, String reason) {
        Refund failedRefund = Refund.createFailed(
                payment, payment.getAmount(), reason
        );

        refundRepository.save(failedRefund);
    }

}
