package com.bootcamp.paymentdemo.payment.dto;

import lombok.Getter;
import java.math.BigDecimal;

@Getter
public class PaymentCreateRequest {

    private String orderNumber;
    private BigDecimal totalAmount;
    private BigDecimal pointsToUse;
}