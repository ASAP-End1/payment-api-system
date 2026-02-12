package com.bootcamp.paymentdemo.refund.dto;

import com.bootcamp.paymentdemo.refund.consts.RefundStatus;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RefundResponse {

    private Long orderId;
    private RefundStatus status;
}
