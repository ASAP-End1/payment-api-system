package com.bootcamp.paymentdemo.external.portone.client;

import com.bootcamp.paymentdemo.config.PortOneProperties;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.external.portone.exception.PortOneException;
import com.bootcamp.paymentdemo.external.portone.dto.PortOneRefundRequest;
import com.bootcamp.paymentdemo.external.portone.dto.PortOneRefundResponse;
import com.bootcamp.paymentdemo.external.portone.error.PortOneErrorCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class PortOneRefundClient {

    private final RestTemplate restTemplate;
    private final PortOneProperties portOneProperties;

    // PortOne API 요청
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
