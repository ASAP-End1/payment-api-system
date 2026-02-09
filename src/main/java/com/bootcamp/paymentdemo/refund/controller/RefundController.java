package com.bootcamp.paymentdemo.refund.controller;

import com.bootcamp.paymentdemo.refund.dto.RefundRequest;
import com.bootcamp.paymentdemo.refund.dto.RefundResponse;
import com.bootcamp.paymentdemo.refund.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/refunds")
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/{id}")
    public ResponseEntity<RefundResponse> refundAll(@PathVariable Long id, @Valid @RequestBody RefundRequest refundRequest) {
        return ResponseEntity.ok(refundService.refundAll(id, refundRequest));
    }
}
