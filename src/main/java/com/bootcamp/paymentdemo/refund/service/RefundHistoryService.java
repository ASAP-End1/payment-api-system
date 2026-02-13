package com.bootcamp.paymentdemo.refund.service;

import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.repository.PaymentRepository;
import com.bootcamp.paymentdemo.refund.entity.Refund;
import com.bootcamp.paymentdemo.refund.exception.RefundException;
import com.bootcamp.paymentdemo.refund.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.bootcamp.paymentdemo.refund.consts.ErrorEnum.ERR_PAYMENT_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class RefundHistoryService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;

    // 환불 요청 이력 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveRequestHistory(String dbPaymentId, String reason, String refundGroupId) {
        Payment payment = paymentRepository.findByDbPaymentId(dbPaymentId).orElseThrow(
                () -> new RefundException(ERR_PAYMENT_NOT_FOUND)
        );
        Refund requestRefund = Refund.createRequest(
                payment.getId(), payment.getTotalAmount(), reason,  refundGroupId
        );

        refundRepository.save(requestRefund);
    }

    // 환불 실패 이력 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailHistory(String dbPaymentId, String reason, String portOneRefundId, String refundGroupId) {
        Payment payment = paymentRepository.findByDbPaymentId(dbPaymentId).orElseThrow(
                () -> new RefundException(ERR_PAYMENT_NOT_FOUND)
        );
        Refund failedRefund = Refund.createFailed(
                payment.getId(), payment.getTotalAmount(), reason,  portOneRefundId,  refundGroupId
        );

        refundRepository.save(failedRefund);
    }

}
