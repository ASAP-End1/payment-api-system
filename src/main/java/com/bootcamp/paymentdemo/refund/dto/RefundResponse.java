package com.bootcamp.paymentdemo.refund.dto;

import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RefundResponse {

    private Long orderId;
}
