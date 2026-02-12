package com.bootcamp.paymentdemo.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateRequest {

    private String orderNumber;
    private BigDecimal totalAmount;
    private BigDecimal pointsToUse;

}