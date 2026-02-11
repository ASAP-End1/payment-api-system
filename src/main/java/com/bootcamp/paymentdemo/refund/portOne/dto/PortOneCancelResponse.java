package com.bootcamp.paymentdemo.refund.portOne.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PortOneCancelResponse {

    @JsonProperty("cancellation")
    private PaymentCancellation cancellation;

    private String type;
    private String message;

    @Getter
    @NoArgsConstructor
    public static class PaymentCancellation {

        private String status;

        private String id;

        private String reason;

        private Long  totalAmount;

        private Long taxFreeAmount;

        private Long vatAmount;

        private String  requestedAt;

        private String type;

        private String message;
    }
}
