package com.bootcamp.paymentdemo.refund.dto;

import lombok.*;

@Getter
@RequiredArgsConstructor
public class RefundResponse {

    private final Long orderId;
    private final String orderNumber;
}
