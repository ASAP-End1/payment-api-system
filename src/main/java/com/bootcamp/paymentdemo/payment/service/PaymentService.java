package com.bootcamp.paymentdemo.payment.service;

import com.bootcamp.paymentdemo.external.portone.client.PortOneClient;
import com.bootcamp.paymentdemo.external.portone.dto.PortOneCancelRequest;
import com.bootcamp.paymentdemo.external.portone.dto.PortOnePaymentResponse;
import com.bootcamp.paymentdemo.membership.service.MembershipService;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.repository.OrderRepository;
import com.bootcamp.paymentdemo.order.service.OrderService;
import com.bootcamp.paymentdemo.payment.consts.PaymentStatus;
import com.bootcamp.paymentdemo.payment.dto.*;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.repository.PaymentRepository;
import com.bootcamp.paymentdemo.point.service.PointService;
import com.bootcamp.paymentdemo.webhook.dto.PortoneWebhookPayload;
import com.bootcamp.paymentdemo.webhook.entity.WebhookEvent;
import com.bootcamp.paymentdemo.webhook.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PortOneClient portOneClient;
    private final WebhookRepository webhookRepository;
    private final OrderService orderService;

    private final PointService pointService;
    private final MembershipService membershipService;

    @Transactional
    public PaymentCreateResponse createPayment(PaymentCreateRequest request) {

        // 1. 주문 정보 조회 (결제와 연결하기 위함)
        Order order = orderRepository.findByOrderNumber(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 포인트 사용 부분 구현
        BigDecimal pointsToUse = request.getPointsToUse() != null ? request.getPointsToUse() : BigDecimal.ZERO;

        if (pointsToUse.compareTo(BigDecimal.ZERO) > 0) {
            // Order 엔티티에 할인 적용 (finalAmount 갱신)
            order.applyPointDiscount(pointsToUse);
            // 실제 포인트 차감 (수민님 코드 사용)
            pointService.usePoints(order.getUser(), order);
        }

        // 2. merchant_uid 역할
        String dbPaymentId = "order_mid_" + System.currentTimeMillis();

        // 3. 결제 장부 생성 (PENDING 상태)
        Payment payment = Payment.builder()
                .dbPaymentId(dbPaymentId)
                .order(order)
                .totalAmount(order.getTotalAmount()) // 원래 주문 총액
                .pointsToUse(pointsToUse)            // 사용 포인트 기록
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);

        log.info("결제 대기 장부 생성 완료: DB_ID={}, OrderID={}, 실결제액={}",
                dbPaymentId, order.getId(), order.getFinalAmount());

        // 4. 응답 DTO의 paymentId 칸에 우리가 만든 dbPaymentId를 담아서 보냄 (PortOne SDK용)
        return new PaymentCreateResponse(true, dbPaymentId, "PENDING");
    }

    @Transactional
    public PaymentConfirmResponse confirmPayment(String dbPaymentId, String impUid) {
        // 예외 발생 시 보상 트랜잭션에서 쓰기 위해 변수 선언을 밖으로 뺌
        Payment payment = null;

        try {
            // 1. DB에서 결제 대기 중인 DB 먼저 조회
            payment = paymentRepository.findByDbPaymentId(dbPaymentId)
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

            // 4. 금액 검증: DB 금액(포인트 차감 후 최종금액)과 포트원 실제 결제 금액 비교
            BigDecimal expectedPayAmount = payment.getOrder().getFinalAmount();
            BigDecimal actualPayAmount = portOneResponse.amount().total();

            if (expectedPayAmount.compareTo(actualPayAmount) != 0) {
                log.error("금액 불일치! DB(예상): {}, PortOne(실제): {}", expectedPayAmount, actualPayAmount);

                if (payment.getPointsToUse().compareTo(BigDecimal.ZERO) > 0) {
                    try {
                        pointService.refundPoints(payment.getOrder().getUser(), payment.getOrder());
                    } catch (Exception px) {
                        log.error("금액 불일치로 인한 포인트 롤백 실패: {}", px.getMessage());
                    }
                }

                // 금액 위변조 시 즉시 취소
                portOneClient.cancelPayment(impUid, PortOneCancelRequest.fullCancel("금액 위변조 감지"));
                return new PaymentConfirmResponse(false, null, "AMOUNT_MISMATCH");
            }

            // 5. 상태 업데이트 (PAID)
            try {
                payment.completePayment(impUid);

                log.info("결제 최종 확정: {}", dbPaymentId);

                // 6. 주문 상태 업데이트 (PENDING_PAYMENT -> PENDING_CONFIRMATION)
                orderService.completePayment(payment.getOrder().getId());
                log.info("주문 상태 업데이트 완료: orderId={}", payment.getOrder().getId());
            } catch (Exception e) {
                // 7. 보상 트랜잭션: 내부 DB 반영 실패 시 포트원 결제 강제 취소  *** 중요한 로직
                log.error("내부 처리 실패로 인한 결제 보상 취소 진행: {}", e.getMessage());

                // 구현 예정

                // PG사 결제 취소
                portOneClient.cancelPayment(impUid, PortOneCancelRequest.fullCancel("서버 내부 오류로 인한 자동 취소"));
                throw e;
            }

            return new PaymentConfirmResponse(true, payment.getOrder().getId().toString(), "PAID");

        } catch (Exception e) {
            log.error("결제 확정 중 오류 발생: {}", e.getMessage());
            return new PaymentConfirmResponse(false, null, "FAILED");
        }
    }

    public void processWebhook(PortoneWebhookPayload payload, String webhookId) {
        try {
            // 1. 웹훅 이벤트 기록
            saveWebhookEvent(payload, webhookId);

            log.info("[WEBHOOK] 이벤트 기록 성공: {}", webhookId);

            //  2. 비즈니스 로직 실행 전 ID 매핑
            String portOnePaymentId = payload.getData().getPaymentId();
            PortOnePaymentResponse details = portOneClient.getPayment(portOnePaymentId);

            // PortOne V2 응답의 id 필드가 우리가 보낸 dbPaymentId(merchant_uid)
            String dbPaymentId = details.id();

            // 매핑된 ID로 확정 로직 수행
            confirmPayment(dbPaymentId, portOnePaymentId);

            log.info("[WEBHOOK] 모든 로직 처리 완료: {}", webhookId);

        } catch (DataIntegrityViolationException e) {
            // 3. 중복 호출 방어 (PortOne에서 자꾸 호출을 2번해서 Duplicate오류 발생했었음 / 트러블슈터 활용
            log.warn("[WEBHOOK] 2중 호출 웹훅 무시 : {}", webhookId);

        } catch (Exception e) {
            // 4. 예외 발생 시 로그 띄우기
            log.error("[WEBHOOK] 기타 비즈니스 로직 처리 중 오류: {}", e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveWebhookEvent(PortoneWebhookPayload payload, String webhookId) {
        WebhookEvent event = WebhookEvent.builder()
                .webhookId(webhookId)
                .paymentId(payload.getData().getPaymentId())
                .eventStatus(payload.getType())
                .build();

        webhookRepository.saveAndFlush(event);
    }
}