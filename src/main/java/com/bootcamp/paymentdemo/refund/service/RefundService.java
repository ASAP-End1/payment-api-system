package com.bootcamp.paymentdemo.refund.service;

import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.repository.PaymentRepository;
import com.bootcamp.paymentdemo.refund.dto.RefundRequest;
import com.bootcamp.paymentdemo.refund.dto.RefundResponse;
import com.bootcamp.paymentdemo.refund.entity.Refund;
import com.bootcamp.paymentdemo.refund.exception.PortOneException;
import com.bootcamp.paymentdemo.refund.exception.RefundException;
import com.bootcamp.paymentdemo.refund.portOne.client.PortOneRefundClient;
import com.bootcamp.paymentdemo.refund.repository.RefundRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.bootcamp.paymentdemo.refund.consts.ErrorEnum.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final RefundHistoryService refundHistoryService;
    private final PaymentRepository paymentRepository;
    private final PortOneRefundClient portOneRefundClient;

    @Transactional
    public RefundResponse refundAll(Long id, @Valid RefundRequest refundRequest) {

        Payment lockedPayment = paymentRepository.findByIdWithLock(id).orElseThrow(
                () -> new RefundException(ERR_PAYMENT_NOT_FOUND)
        );

        validateRefundable(lockedPayment);

        String refundGroupId = "rfnd-grp-" + UUID.randomUUID();
        Long orderId = lockedPayment.getOrder().getId();

        refundHistoryService.saveRequestHistory(lockedPayment, refundRequest.getReason(), refundGroupId);

        String portOneRefundId = null;

        try {

           portOneRefundId = portOneRefundClient.cancelPayment(lockedPayment, refundRequest.getReason());

            completeRefund(lockedPayment, refundRequest.getReason(), portOneRefundId, refundGroupId);

            return RefundResponse.success(orderId);

        } catch (PortOneException e) {
            log.error("PortOne API 호출 실패 - Payment ID: {}, Refund Group ID: {}", id, refundGroupId, e);

            refundHistoryService.saveFailHistory(lockedPayment, refundRequest.getReason(), portOneRefundId, refundGroupId);

            return RefundResponse.fail(orderId);

        } catch (Exception e) {
            log.error("서버 내부 오류 발생 - Payment ID: {}, Refund Group ID: {}",  id, refundGroupId, e);

            refundHistoryService.saveFailHistory(lockedPayment, refundRequest.getReason(), portOneRefundId, refundGroupId);

            throw e;
        }

    }

    // 환불 가능 상태 검증
    private void validateRefundable(Payment payment) {

        if(payment.getOrder().isCanceled()) {
            throw new RefundException(ERR_REFUND_ALREADY_PROCESSED);
        }

        if(payment.isRefund()) {
            throw new RefundException(ERR_REFUND_ALREADY_PROCESSED);
        }

        if(!payment.getOrder().isAwaitingConfirmation()) {
            throw new RefundException(ERR_REFUND_INVALID_STATUS);
        }

        if(!payment.isCompleted()) {
            throw new RefundException(ERR_REFUND_INVALID_STATUS);
        }

    }

    // 환불 완료 이력 저장
    private void completeRefund(Payment payment, String reason, String portOneRefundId, String refundGroupId) {
        Refund completedRefund = Refund.createCompleted(
                payment, payment.getTotalAmount(), reason,  portOneRefundId,  refundGroupId
        );

        refundRepository.save(completedRefund);

        payment.refund();
        payment.getOrder().cancel();

        /*
            재고 복구(Order -> OrderProduct -> Product를 거치는 메서드 필요)
         */

    }
}
