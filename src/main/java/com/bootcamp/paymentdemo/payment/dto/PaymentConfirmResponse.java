package com.bootcamp.paymentdemo.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class PaymentConfirmResponse {
    private final boolean success;
    private final String orderId;
    private final String status;

    PaymentConfirmResponse(boolean success, String orderId, String status) {
        this.success = success;
        this.orderId = orderId;
        this.status = status;
    }


    /*
     일단 지금으로써는 yml 파일과 모양새를 통일하기 위해서 구현해 둔것, 아마 중간에 다듬을 가능성이 높음
     */
}