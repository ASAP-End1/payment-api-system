package com.bootcamp.paymentdemo.refund.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RefundStatus {

    PENDING("PENDING", "환불 요청"),
    COMPLETED("COMPLETED", "환불 완료"),
    FAILED("FAILED", "환불 실패");

    private final String code;
    private final String displayName;
}
