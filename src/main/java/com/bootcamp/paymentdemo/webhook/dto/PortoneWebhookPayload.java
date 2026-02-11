package com.bootcamp.paymentdemo.webhook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortoneWebhookPayload {

    @JsonProperty("type")
    private String type;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("data")
    private Data data;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        @JsonProperty("transactionId")
        private String transactionId;

        @JsonProperty("paymentId")
        private String paymentId;

        @JsonProperty("storeId")
        private String storeId;
    }
}