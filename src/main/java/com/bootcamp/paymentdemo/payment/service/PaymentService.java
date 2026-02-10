package com.bootcamp.paymentdemo.payment.service;

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
            // 1. 포트원 서버에서 실제 결제 내역 조회 (검증 시작)
            PortOnePaymentResponse portOneResponse = portOneClient.getPayment(impUid);

            // 2. DB에서 결제 대기 중인 장부 조회
            Payment payment = paymentRepository.findByDbPaymentId(dbPaymentId)
                    .orElseThrow(() -> new IllegalArgumentException("결제 건이 존재하지 않습니다."));

            // 3. 금액 검증: DB 금액과 포트원 실제 결제 금액 비교 (위변조 방지)
            if (payment.getTotalAmount().compareTo(portOneResponse.amount().total()) != 0) {
                log.error("금액 불일치! DB: {}, PortOne: {}", payment.getTotalAmount(), portOneResponse.amount().total());
                return new PaymentConfirmResponse(false, null, "AMOUNT_MISMATCH");
            }

            // 4. 상태 업데이트 (PAID)
            payment.completePayment(impUid);
            log.info("결제 최종 확정: {}", dbPaymentId);

            return new PaymentConfirmResponse(true, payment.getOrder().getId().toString(), "PAID");

        } catch (Exception e) {
            log.error("결제 확정 중 오류 발생: {}", e.getMessage());
            return new PaymentConfirmResponse(false, null, "FAILED");
        }
    }
}