package com.bootcamp.paymentdemo.external.portone.client;

import com.bootcamp.paymentdemo.config.PortOneProperties;
import com.bootcamp.paymentdemo.external.portone.dto.*;
import com.bootcamp.paymentdemo.external.portone.error.PortOneError;
import com.bootcamp.paymentdemo.external.portone.error.PortOneErrorCase;
import com.bootcamp.paymentdemo.external.portone.exception.PortOneApiException;
import com.bootcamp.paymentdemo.external.portone.exception.PortOneException;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class PortOneClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final PortOneProperties portOneProperties;

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

    // PortOne 환불 API 요청
    public String cancelPayment(Payment payment, String reason) {

        String portOnePaymentId = payment.getPaymentId();

        if (portOnePaymentId == null || portOnePaymentId.isEmpty()) {
            throw new PortOneException(HttpStatus.BAD_REQUEST, "PortOne Payment ID가 존재하지 않습니다");
        }

        String url = portOneProperties.getApi().getBaseUrl() + "/payments/" +  portOnePaymentId +  "/cancel";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "PortOne " + portOneProperties.getApi().getSecret());

        PortOneRefundRequest portOneCancelRequest = new PortOneRefundRequest(
                portOneProperties.getStore().getId(),
                reason
        );

        HttpEntity<PortOneRefundRequest> request = new HttpEntity<>(portOneCancelRequest, headers);

        try {
            ResponseEntity<PortOneRefundResponse> response = restTemplate.exchange(url, HttpMethod.POST, request, PortOneRefundResponse.class);

            PortOneRefundResponse portOneCancelResponse = response.getBody();

            if (portOneCancelResponse == null || portOneCancelResponse.getCancellation() == null) {
                throw new PortOneException(HttpStatus.INTERNAL_SERVER_ERROR, "PortOne API 응답이 올바르지 않습니다");
            }

            PortOneRefundResponse.PaymentCancellation paymentCancellation = portOneCancelResponse.getCancellation();

            if (!"SUCCEEDED".equals(paymentCancellation.getStatus())) {
                HttpStatus httpStatus = PortOneErrorCase.caseToHttpStatus(paymentCancellation.getType());
                throw new PortOneException(httpStatus, paymentCancellation.getMessage());
            }

            return paymentCancellation.getId();

        } catch (HttpStatusCodeException e) {
            HttpStatus httpStatus = HttpStatus.valueOf(e.getStatusCode().value());
            throw new PortOneException(httpStatus, e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new PortOneException(HttpStatus.INTERNAL_SERVER_ERROR, "PortOne 호출 중 알 수 없는 오류");
        }
    }
}