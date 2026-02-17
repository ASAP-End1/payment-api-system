package com.bootcamp.paymentdemo.refund.service;

import com.bootcamp.paymentdemo.payment.repository.PaymentRepository;
import com.bootcamp.paymentdemo.refund.entity.Refund;
import com.bootcamp.paymentdemo.refund.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class RefundHistoryService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;

    // 환불 요청 이력 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveRequestHistory(Long paymentId, BigDecimal amount, String reason, String refundGroupId) {
        Refund requestRefund = Refund.createRequest(
                paymentId, amount, reason,  refundGroupId
        );

        refundRepository.save(requestRefund);
    }

    // 환불 실패 이력 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailHistory(Long paymentId, BigDecimal amount, String reason, String portOneRefundId, String refundGroupId) {
        Refund failedRefund = Refund.createFailed(
                paymentId, amount, reason,  portOneRefundId,  refundGroupId
        );

        refundRepository.save(failedRefund);
    }

}
