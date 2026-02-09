package com.bootcamp.paymentdemo.payment.service;

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

    // 결제 준비 Order랑 다른 부분들 개발 되면 이후 추가하겠음
    @Transactional
    public PaymentCreateResponse createPayment(PaymentCreateRequest request) {
        // 1. 주문 정보 조회 (결제와 연결하기 위함)
        Order order = orderRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 2. [우리 서버 ID 생성] merchant_uid 역할
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
    public PaymentConfirmResponse confirmPayment(String dbPaymentId, String impUid) { // 인자명도 dbPaymentId로 통일
        try {
            // [1] DB 조회: 우리가 발행한 번호(dbPaymentId)로 결제 건을 찾습니다.
            Payment payment = paymentRepository.findByDbPaymentId(dbPaymentId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 결제 준비 건이 존재하지 않습니다: " + dbPaymentId));

            // [2] 상태 업데이트: 엔티티에 정의한 completePayment 사용
            // 이 메서드 내부에서 status = PAID와 paymentId = impUid가 동시에 처리됩니다.
            payment.completePayment(impUid);

            log.info("결제 확정 완료: DB_ID={}, PortOne_ID={}", dbPaymentId, impUid);

            // [3] 응답 반환: Long 타입 ID를 String으로 변환하여 DTO 생성
            return new PaymentConfirmResponse(
                    true,
                    payment.getOrder().getId().toString(),
                    "PAID"
            );

        } catch (Exception e) {
            log.error("결제 확정 처리 중 오류: {}", e.getMessage());
            return new PaymentConfirmResponse(false, null, "FAILED");
        }
    }
}