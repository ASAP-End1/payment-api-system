package com.bootcamp.paymentdemo.external.portone.client;

import com.bootcamp.paymentdemo.external.portone.dto.*;
import com.bootcamp.paymentdemo.external.portone.error.PortOneError;
import com.bootcamp.paymentdemo.external.portone.error.PortOneErrorCase;
import com.bootcamp.paymentdemo.external.portone.exception.PortOneApiException;
import com.bootcamp.paymentdemo.external.portone.exception.PortOneException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class PortOneClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PortOnePaymentResponse createInstantPayment(String paymentId, PortOnePaymentRequest request) {
        return restClient.post()
                .uri("/payments/{paymentId}/instant", paymentId)
                .header("Idempotency-Key", paymentId)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    PortOneError error = parseErrorResponse(res);
                    throw new PortOneApiException(
                            error != null ? error.type() : "UNKNOWN_ERROR",
                            error != null ? error.message() : "Unknown error occurred",
                            res.getStatusCode().value()
                    );
                })
                .body(PortOnePaymentResponse.class);
    }

    public PortOnePaymentResponse getPayment(String paymentId) {
        return restClient.get()
                .uri("/payments/{paymentId}", paymentId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    PortOneError error = parseErrorResponse(res);
                    throw new PortOneApiException(
                            error != null ? error.type() : "UNKNOWN_ERROR",
                            error != null ? error.message() : "Unknown error occurred",
                            res.getStatusCode().value()
                    );
                })
                .body(PortOnePaymentResponse.class);
    }

    public PortOnePaymentResponse cancelPayment(String paymentId, PortOneCancelRequest request) {
        return restClient.post()
                .uri("/payments/{paymentId}/cancel", paymentId)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    PortOneError error = parseErrorResponse(res);
                    throw new PortOneApiException(
                            error != null ? error.type() : "UNKNOWN_ERROR",
                            error != null ? error.message() : "Unknown error occurred",
                            res.getStatusCode().value()
                    );
                })
                .body(PortOnePaymentResponse.class);
    }

    private PortOneError parseErrorResponse(org.springframework.http.client.ClientHttpResponse response) {
        try {
            return objectMapper.readValue(response.getBody(), PortOneError.class);
        } catch (Exception e) {
            return null;
        }
    }


    public PortOneRefundResponse refundPayment(String paymentId, PortOneRefundRequest request) {
        try {
            return restClient.post()
                    .uri("/payments/{paymentId}/cancel", paymentId)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                       PortOneError error = parseErrorResponse(res);
                       throw new PortOneException(
                               PortOneErrorCase.caseToHttpStatus(error.type()), error.message()
                       );
                    }).body(PortOneRefundResponse.class);
        } catch (PortOneException e) {
            throw e;
        } catch (Exception e) {
            throw new PortOneException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "PortOne 호출 중 알 수 없는 오류"
            );
        }
    }
}