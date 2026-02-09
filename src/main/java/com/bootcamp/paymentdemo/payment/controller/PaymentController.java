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
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;


    @PostMapping
    public PaymentCreateResponse createPayment(@RequestBody PaymentCreateRequest request) {
        log.info("결제 생성 요청: OrderID={}", request.getOrderId());
        return paymentService.createPayment(request);
    }
//
//    @PostMapping("/{paymentId}/confirm")
//    public PaymentConfirmResponse confirmPayment(@PathVariable String paymentId) {
//        log.info("결제 확정 요청: PaymentID(imp_uid)={}", paymentId);
//        return paymentService.confirmPayment(paymentId);
//    }

}