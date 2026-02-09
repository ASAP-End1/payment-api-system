package com.bootcamp.paymentdemo.domain.refund.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RefundRequest {

    @NotBlank(message = "환불 사유는 필수입니다")
    private String reason;
}
