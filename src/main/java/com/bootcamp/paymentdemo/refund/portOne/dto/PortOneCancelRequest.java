package com.bootcamp.paymentdemo.refund.portOne.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PortOneCancelRequest {

    private String storeId;
    private String reason;
}
