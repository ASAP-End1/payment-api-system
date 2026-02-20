package com.bootcamp.paymentdemo.payment.controller;

import com.bootcamp.paymentdemo.common.dto.ApiResponse;
import com.bootcamp.paymentdemo.payment.dto.PaymentConfirmResponse;
import com.bootcamp.paymentdemo.payment.dto.PaymentCreateRequest;
import com.bootcamp.paymentdemo.payment.dto.PaymentCreateResponse;
import com.bootcamp.paymentdemo.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;


    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PaymentCreateResponse>> createPayment(@RequestBody PaymentCreateRequest request) {
        log.info("결제 생성 요청: orderNumber={}", request.getOrderNumber());
        PaymentCreateResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "결제 생성 성공", response));
    }


    @PostMapping("/{dbPaymentId}/confirm")
    public ResponseEntity<ApiResponse<PaymentConfirmResponse>> confirmPayment(
            @PathVariable String dbPaymentId) {

        log.info("결제 확정 요청: dbPaymentId={}", dbPaymentId);
        PaymentConfirmResponse response = paymentService.confirmPayment(dbPaymentId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK, "결제 확정 성공", response));
    }
}
