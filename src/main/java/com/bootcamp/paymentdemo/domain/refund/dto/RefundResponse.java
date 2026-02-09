package com.bootcamp.paymentdemo.domain.refund.dto;

import com.bootcamp.paymentdemo.domain.refund.entity.RefundStatus;
import lombok.Builder;
import lombok.Getter;

import javax.naming.ldap.Rdn;

@Getter
@Builder
public class RefundResponse {

    private final boolean success;
    private final Long orderId;
    private final RefundStatus status;

    public static RefundResponse success(Long orderId) {
        return RefundResponse.builder()
                .success(true)
                .orderId(orderId)
                .status(RefundStatus.COMPLETED)
                .build();
    }

    public static RefundResponse fail(Long orderId) {
        return RefundResponse.builder()
                .success(false)
                .orderId(orderId)
                .status(RefundStatus.FAILED)
                .build();
    }
}
