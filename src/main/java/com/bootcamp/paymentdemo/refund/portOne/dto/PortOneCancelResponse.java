package com.bootcamp.paymentdemo.refund.portOne.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PortOneCancelResponse {

    @JsonProperty("cancellation")
    private PaymentCancellation cancellation;

    @Getter
    @NoArgsConstructor
    public static class PaymentCancellation {

        @JsonProperty("status")
        private String status;

        @JsonProperty("id")
        private String id;

        @JsonProperty("reason")
        private String reason;

        @JsonProperty("totalAmount")
        private Long  totalAmount;

        @JsonProperty("taxFreeAmount")
        private Long taxFreeAmount;

        @JsonProperty("vatAmount")
        private Long vatAmount;

        @JsonProperty("requestedAt")
        private String  requestedAt;
    }
}
