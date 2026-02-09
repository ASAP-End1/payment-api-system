package com.bootcamp.paymentdemo.domain.refund.controller;

import com.bootcamp.paymentdemo.domain.refund.dto.RefundRequest;
import com.bootcamp.paymentdemo.domain.refund.dto.RefundResponse;
import com.bootcamp.paymentdemo.domain.refund.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/refunds")
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/{paymentId}")
    public ResponseEntity<RefundResponse> refundAll(@PathVariable Long paymentId, @Valid @RequestBody RefundRequest refundRequest) {
        return ResponseEntity.ok(refundService.refundAll(paymentId, refundRequest));
    }
}
