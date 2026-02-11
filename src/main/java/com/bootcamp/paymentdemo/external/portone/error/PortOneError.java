package com.bootcamp.paymentdemo.external.portone.error;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneError(
        String type,
        String message
) {
}