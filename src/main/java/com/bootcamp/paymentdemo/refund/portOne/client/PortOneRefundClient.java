package com.bootcamp.paymentdemo.refund.portOne.client;

import com.bootcamp.paymentdemo.config.PortOneProperties;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.refund.portOne.dto.PortOneCancelRequest;
import com.bootcamp.paymentdemo.refund.portOne.dto.PortOneCancelResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
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
            throw new IllegalArgumentException("에러코드: 해당 PortOne Payment ID가 존재하지 않습니다.");
        }

        String url = portOneProperties.getApi().getBaseUrl() + "/payments/" +  portOnePaymentId +  "/cancel";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "PortOne " + portOneProperties.getApi().getSecret());

        PortOneCancelRequest portOneCancelRequest = new PortOneCancelRequest(
                portOneProperties.getStore().getId(),
                reason
        );

        HttpEntity<PortOneCancelRequest> request = new HttpEntity<>(portOneCancelRequest, headers);

        try {
            ResponseEntity<PortOneCancelResponse> response = restTemplate.exchange(url, HttpMethod.POST, request, PortOneCancelResponse.class);

            PortOneCancelResponse portOneCancelResponse = response.getBody();

            if (portOneCancelResponse == null || portOneCancelResponse.getCancellation() == null) {
                throw new IllegalArgumentException("에러코드: PortOne API 응답이 없습니다");
            }

            PortOneCancelResponse.PaymentCancellation paymentCancellation = portOneCancelResponse.getCancellation();

            if (!"SUCCEEDED".equals(paymentCancellation.getStatus())) {
                throw new RuntimeException("에러코드: " + paymentCancellation.getReason());
            }

            return paymentCancellation.getId();

        } catch (Exception e) {
            throw new IllegalArgumentException("에러코드: PortOne 환불 처리 중 오류가 발생했습니다");
        }
    }

}
