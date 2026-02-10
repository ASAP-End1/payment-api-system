package com.bootcamp.paymentdemo.payment.service;

import com.bootcamp.paymentdemo.external.portone.PortOneCancelRequest;
import com.bootcamp.paymentdemo.external.portone.PortOneClient;
import com.bootcamp.paymentdemo.external.portone.PortOnePaymentResponse;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.repository.OrderRepository;
import com.bootcamp.paymentdemo.payment.consts.PaymentStatus;
import com.bootcamp.paymentdemo.payment.dto.PaymentConfirmResponse;
import com.bootcamp.paymentdemo.payment.dto.PaymentCreateRequest;
import com.bootcamp.paymentdemo.payment.dto.PaymentCreateResponse;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PortOneClient portOneClient;

    @Transactional
    public PaymentCreateResponse createPayment(PaymentCreateRequest request) {
        // 1. 주문 정보 조회 (결제와 연결하기 위함)
        Order order = orderRepository.findByOrderNumber(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 2. merchant_uid 역할
        String dbPaymentId = "order_mid_" + System.currentTimeMillis();

        // 3. 결제 장부 생성 (PENDING 상태)
        Payment payment = Payment.builder()
                .dbPaymentId(dbPaymentId)
                .order(order)
                .totalAmount(request.getTotalAmount())
                .pointsToUse(request.getPointsToUse())
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);

        log.info("결제 대기 장부 생성 완료: DB_ID={}, OrderID={}", dbPaymentId, order.getId());

        // 4. 응답 DTO의 paymentId 칸에 우리가 만든 dbPaymentId를 담아서 보냄 (PortOne SDK용)
        return new PaymentCreateResponse(true, dbPaymentId, "PENDING");
    }


    @Transactional
    public PaymentConfirmResponse confirmPayment(String dbPaymentId, String impUid) {
        try {
            // 1. DB에서 결제 대기 중인 DB 먼저 조회
            Payment payment = paymentRepository.findByDbPaymentId(dbPaymentId)
                    .orElseThrow(() -> new IllegalArgumentException("결제 건이 존재하지 않습니다."));

            // 2. 멱등성 체크: 저거 뭐지? 상태가 대기중이 아니면 2중으로 시도한거기때문에 멱등성 체크로 확인
            if (payment.getStatus() != PaymentStatus.PENDING) {
                log.info("이미 처리된 결제 건입니다. 상태: {}, DB_ID={}", payment.getStatus(), dbPaymentId);
                return new PaymentConfirmResponse(
                        true,
                        payment.getOrder().getId().toString(),
                        payment.getStatus().name()
                );
            }

            // 3. 검증 시작
            PortOnePaymentResponse portOneResponse = portOneClient.getPayment(impUid);

            // 4. 금액 검증: DB 금액과 포트원 실제 결제 금액 비교
            if (payment.getTotalAmount().compareTo(portOneResponse.amount().total()) != 0) {
                log.error("금액 불일치! DB: {}, PortOne: {}", payment.getTotalAmount(), portOneResponse.amount().total());
                return new PaymentConfirmResponse(false, null, "AMOUNT_MISMATCH");
            }

            // 5. 상태 업데이트 (PAID)
            try {
                payment.completePayment(impUid);
                log.info("결제 최종 확정: {}", dbPaymentId);
            } catch (Exception e) {
                // 6. 보상 트랜잭션: 내부 DB 반영 실패 시 포트원 결제 강제 취소  *** 중요한 로직
                log.error("내부 처리 실패로 인한 결제 보상 취소 진행: {}", e.getMessage());
                portOneClient.cancelPayment(impUid, PortOneCancelRequest.fullCancel("서버 내부 오류로 인한 자동 취소"));
                throw e; //
            }

            return new PaymentConfirmResponse(true, payment.getOrder().getId().toString(), "PAID");

        } catch (Exception e) {
            log.error("결제 확정 중 오류 발생: {}", e.getMessage());
            return new PaymentConfirmResponse(false, null, "FAILED");
        }
    }
}