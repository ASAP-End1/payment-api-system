package com.bootcamp.paymentdemo.domain.refund.service;

import com.bootcamp.paymentdemo.domain.refund.dto.RefundRequest;
import com.bootcamp.paymentdemo.domain.refund.dto.RefundResponse;
import com.bootcamp.paymentdemo.domain.refund.entity.Refund;
import com.bootcamp.paymentdemo.domain.refund.repository.RefundRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final RefundHistoryService refundHistoryService;
    private final PaymentRepository paymentRepository;

    public RefundResponse refundAll(Long paymentId, @Valid RefundRequest refundRequest) {

        Payment payment = paymentRepository.findById(paymentId).orElseThrow(
                () -> new IllegalArgumentException("에러코드: 설명")
        );

        validateRefundable(payment);

        Long orderId = payment.getOrder().getOrderId();

        refundHistoryService.saveRequestHistory(payment, refundRequest.getReason());

        try {

            /*
                PortOne API 호출 로직 작성
             */

            completeRefund(payment, refundRequest.getReason());

            return RefundResponse.success(orderId);
        } catch (Exception e) {

            refundHistoryService.saveFailHistory(payment, refundRequest.getReason());

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
    public void completeRefund(Payment payment, String reason) {
        Refund completedRefund = Refund.createCompleted(
                payment, payment.getAmount(), reason
        );

        refundRepository.save(completedRefund);

        /*
            결제 상태 변경 (PAID -> REFUND)
            주문 상태 변경 (PENDING_CONFIRMATION -> CANCELLED)
            재고 복구(Order Entity에 재고 증가, 감소 메소드 필요할 듯)
         */

    }
}
