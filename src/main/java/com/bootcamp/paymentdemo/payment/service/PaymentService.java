package com.bootcamp.paymentdemo.payment.service;

import com.bootcamp.paymentdemo.external.portone.client.PortOneClient;
import com.bootcamp.paymentdemo.external.portone.dto.PortOneCancelRequest;
import com.bootcamp.paymentdemo.external.portone.dto.PortOnePaymentResponse;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.repository.OrderRepository;
import com.bootcamp.paymentdemo.order.service.OrderService;
import com.bootcamp.paymentdemo.payment.consts.PaymentStatus;
import com.bootcamp.paymentdemo.payment.dto.*;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.repository.PaymentRepository;
import com.bootcamp.paymentdemo.user.entity.UserPointBalance;
import com.bootcamp.paymentdemo.user.repository.UserPointBalanceRepository;
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
    private final UserPointBalanceRepository userPointBalanceRepository;

    @Transactional
    public PaymentCreateResponse createPayment(PaymentCreateRequest request) {

        BigDecimal pointsToUse = request.getPointsToUse() != null ? request.getPointsToUse() : BigDecimal.ZERO;

        Order order = orderRepository.findByOrderNumber(request.getOrderNumber())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        if (pointsToUse.compareTo(BigDecimal.ZERO) > 0) {

            UserPointBalance userPointBalance = userPointBalanceRepository.findById(order.getUser().getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("사용자의 포인트 정보를 찾을 수 없습니다."));

            if (userPointBalance.getCurrentPoints().compareTo(pointsToUse) < 0) {
                throw new IllegalArgumentException("포인트 잔액이 부족합니다.");
            }

            order.applyPointDiscount(pointsToUse);
        }

        // 2. 결제 장부 생성 (PENDING)
        String dbPaymentId = "order_mid_" + System.currentTimeMillis();

        Payment payment = Payment.builder()
                .dbPaymentId(dbPaymentId)
                .order(order)
                .totalAmount(order.getTotalAmount())
                .pointsToUse(pointsToUse)
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);

        log.info("결제 대기 장부 생성: DB_ID={}, OrderID={}", dbPaymentId, order.getId());

        return new PaymentCreateResponse(true, dbPaymentId, "PENDING");
    }

    @Transactional
    public PaymentConfirmResponse confirmPayment(String dbPaymentId) {
        Payment payment = null;

        try {
            // 1. 조회 및 멱등성 체크
            payment = paymentRepository.findByDbPaymentId(dbPaymentId)
                    .orElseThrow(() -> new IllegalArgumentException("결제 건이 존재하지 않습니다."));

            if (payment.getStatus() != PaymentStatus.PENDING) {
                return new PaymentConfirmResponse(true, payment.getOrder().getId(),payment.getOrder().getOrderNumber(), payment.getStatus().name());
            }

            // 2. PortOne 검증
            PortOnePaymentResponse portOneResponse = portOneClient.getPayment(dbPaymentId);

            // 3. 금액 검증
            BigDecimal expectedPayAmount = payment.getOrder().getFinalAmount();
            BigDecimal actualPayAmount = portOneResponse.amount().total();

            if (expectedPayAmount.compareTo(actualPayAmount) != 0) {
                log.error("금액 불일치! DB(예상): {}, PortOne(실제): {}", expectedPayAmount, actualPayAmount);
                try {
                    orderService.rollbackUsedPoint(payment.getOrder().getId());
                    payment.cancelPointUsage();
                } catch (Exception ex) {
                    log.error("금액 불일치 주문 취소 중 오류: {}", ex.getMessage());
                }

                // PG사 결제 취소
                portOneClient.cancelPayment(dbPaymentId, PortOneCancelRequest.fullCancel("금액 위변조 감지"));

                return new PaymentConfirmResponse(false, null, payment.getOrder().getOrderNumber(),"AMOUNT_MISMATCH");
            }
            // 4. 결제 성공 처리
            try {
                payment.completePayment(dbPaymentId);

                orderService.completePayment(payment.getOrder().getId());

                log.info("결제 및 주문 최종 확정 완료: {}", dbPaymentId);

            } catch (Exception e) {
                // 5. 보상 트랜잭션 (롤백)
                log.error("내부 처리 실패, 자동 취소 진행: {}", e.getMessage());

                try {
                    // 주문 취소
                    orderService.rollbackUsedPoint(payment.getOrder().getId());
                    payment.cancelPointUsage();
                } catch (Exception ex) {
                    log.warn("주문 취소 처리 중 오류(이미 취소됨 등): {}", ex.getMessage());
                }

                // PG사 취소
                portOneClient.cancelPayment(dbPaymentId, PortOneCancelRequest.fullCancel("서버 오류 자동 취소"));
                throw e;
            }

            return new PaymentConfirmResponse(true, payment.getOrder().getId(),payment.getOrder().getOrderNumber(), "PAID");

        } catch (Exception e) {
            log.error("결제 확정 실패: {}", e.getMessage());
            return new PaymentConfirmResponse(false, null, payment.getOrder().getOrderNumber(),"FAILED");
        }
    }

    public void processWebhook(PortoneWebhookPayload payload, String webhookId) {
        try {
            saveWebhookEvent(payload, webhookId);
            String portOnePaymentId = payload.getData().getPaymentId();
            PortOnePaymentResponse details = portOneClient.getPayment(portOnePaymentId);
            String dbPaymentId = details.id();
            confirmPayment(dbPaymentId);
        } catch (DataIntegrityViolationException e) {
            log.warn("중복 웹훅 무시");
        } catch (Exception e) {
            log.error("웹훅 처리 오류: {}", e.getMessage());
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
