package com.bootcamp.paymentdemo.refund.service;

import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.repository.PaymentRepository;
import com.bootcamp.paymentdemo.refund.dto.RefundRequest;
import com.bootcamp.paymentdemo.refund.dto.RefundResponse;
import com.bootcamp.paymentdemo.refund.entity.Refund;
import com.bootcamp.paymentdemo.refund.repository.RefundRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final RefundHistoryService refundHistoryService;
    private final PaymentRepository paymentRepository;

    @Transactional
    public RefundResponse refundAll(Long id, @Valid RefundRequest refundRequest) {

        Payment payment = paymentRepository.findByIdWithLock(id).orElseThrow(
                () -> new IllegalArgumentException("에러코드: 설명")
        );

        validateRefundable(payment);

        String refundGroupId = "rfnd-grp-" + UUID.randomUUID().toString();
        Long orderId = payment.getOrder().getId();

        refundHistoryService.saveRequestHistory(payment, refundRequest.getReason(), refundGroupId);

        String mockPortOneRefundId = null;

        try {

            /*
                PortOne API 호출 로직 작성
             */

            // 임의 ID 값
            mockPortOneRefundId = "test-refund-" + System.currentTimeMillis();

            completeRefund(payment, refundRequest.getReason(), mockPortOneRefundId, refundGroupId);

            return RefundResponse.success(orderId);

        } catch (Exception e) {
            if (mockPortOneRefundId == null) {
                // PortOne API 호출 실패
                log.error("PortOne API 호출 실패 - Payment ID: {}, Refund Group ID: {}, Error: {}",
                        id, refundGroupId, e.getMessage(), e);
            } else {
                // 서버 처리 실패
                log.error("서버 처리 실패 - Payment ID: {}, PortOne Refund ID: {}, Error: {}",
                        id, mockPortOneRefundId, e.getMessage(), e);
            }
            refundHistoryService.saveFailHistory(payment, refundRequest.getReason(), mockPortOneRefundId, refundGroupId);

            return RefundResponse.fail(orderId);
        }

    }

    // 환불 가능 상태 검증
    private void validateRefundable(Payment payment) {
        if(!payment.getOrder().isAwaitingConfirmation()) {
            throw new IllegalArgumentException("에러코드: 설명");
        }

        if(!payment.isCompleted()) {
            throw new IllegalArgumentException(("에러코드: 설명"));
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
