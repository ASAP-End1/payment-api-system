package com.bootcamp.paymentdemo.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class PaymentCreateRequest {

    private String orderId;
    private BigDecimal totalAmount;
    private BigDecimal pointsToUse;

}

/*
    플로우 설명

    Request에서 OrderId를 받으면 내부 로직에서 Order 객체를 불러오고 그걸로 결제해야하는 금액과 사용하는 포인트를 PortOneSDK로 보냄
    그 전에 테이블에 현재 화면의 정보들을 미리 삽입시켜둘거임. <- 내부로직에서 구현 할 부분

 */