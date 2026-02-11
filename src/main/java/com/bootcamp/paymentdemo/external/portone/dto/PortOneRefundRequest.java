package com.bootcamp.paymentdemo.external.portone.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PortOneRefundRequest {
    private String reason;
}
