package com.bootcamp.paymentdemo.refund.service;

import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.repository.PaymentRepository;
import com.bootcamp.paymentdemo.refund.dto.RefundRequest;
import com.bootcamp.paymentdemo.refund.dto.RefundResponse;
import com.bootcamp.paymentdemo.refund.entity.Refund;
import com.bootcamp.paymentdemo.refund.repository.RefundRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final RefundHistoryService refundHistoryService;
    private final PaymentRepository paymentRepository;

    @Transactional
    public RefundResponse refundAll(Long paymentId, @Valid RefundRequest refundRequest) {

        Payment payment = paymentRepository.findByIdWithLock(paymentId).orElseThrow(
                () -> new IllegalArgumentException("에러코드: 설명")
        );

        validateRefundable(payment);

        Long orderId = payment.getOrder().getId();

        refundHistoryService.saveRequestHistory(payment, refundRequest.getReason());

        try {

            /*
                PortOne API 호출 로직 작성
             */

            String mockPortOneRefundId = "test-refund-" + System.currentTimeMillis();

            completeRefund(payment, refundRequest.getReason(), mockPortOneRefundId);

            return RefundResponse.success(orderId);

        } catch (Exception e) {

            String mockPortOneRefundId = "test-refund-" + System.currentTimeMillis();
            refundHistoryService.saveFailHistory(payment, refundRequest.getReason(), mockPortOneRefundId);

            return RefundResponse.fail(orderId);
        }

    }

    // 환불 가능 상태 검증
    public void validateRefundable(Payment payment) {
        if(!payment.getOrder().isAwaitingConfirmation()) {
            throw new IllegalArgumentException("에러코드: 설명");
        }

        if(!payment.isCompleted()) {
            throw new IllegalArgumentException(("에러코드: 설명"));
        }
    }

    // 환불 완료 이력 저장
    public void completeRefund(Payment payment, String reason, String portOneRefundId) {
        Refund completedRefund = Refund.createCompleted(
                payment, payment.getTotalAmount(), reason,  portOneRefundId
        );

        refundRepository.save(completedRefund);

        payment.refund();
        payment.getOrder().cancel();

        /*
            재고 복구(Order -> OrderProduct -> Product를 거치는 메서드 필요)
         */

    }
}
