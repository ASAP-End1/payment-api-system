package com.bootcamp.paymentdemo.payment.controller;

import com.bootcamp.paymentdemo.payment.dto.PaymentConfirmResponse;
import com.bootcamp.paymentdemo.payment.dto.PaymentCreateRequest;
import com.bootcamp.paymentdemo.payment.dto.PaymentCreateResponse;
import com.bootcamp.paymentdemo.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    // 1. 결제 준비 (장부 생성)
    @PostMapping("/create")
    public PaymentCreateResponse createPayment(@RequestBody PaymentCreateRequest request) {
        log.info("결제 생성 요청: OrderID={}", request.getOrderId());
        return paymentService.createPayment(request);
    }

    // 2. 결제 확정 (검증 및 상태 변경)
    @PostMapping("/{dbPaymentId}/confirm")
    public PaymentConfirmResponse confirmPayment(
            @PathVariable String dbPaymentId,
            @RequestParam String paymentId) { // 포트원의 imp_uid를 파라미터로 받음

        log.info("결제 확정 요청: DB_ID={}, PortOne_ID={}", dbPaymentId, paymentId);
        return paymentService.confirmPayment(dbPaymentId, paymentId);
    }
}
