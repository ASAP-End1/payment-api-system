package com.bootcamp.paymentdemo.webhook.controller;

import com.bootcamp.paymentdemo.payment.service.PaymentService;
import com.bootcamp.paymentdemo.webhook.dto.PortoneWebhookPayload;
import com.bootcamp.paymentdemo.webhook.service.PortOneSdkWebhookVerifier;
import io.portone.sdk.server.errors.WebhookVerificationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@RestController
@Slf4j
@RequiredArgsConstructor
public class WebhookController {

    private final PortOneSdkWebhookVerifier verifier;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/portone-webhook", consumes = "application/json")
    public ResponseEntity<Void> handlePortoneWebhook(


            @RequestBody byte[] rawBody,


            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-timestamp") String webhookTimestamp,
            @RequestHeader("webhook-signature") String webhookSignature
    ) {
        String bodyString = new String(rawBody, StandardCharsets.UTF_8);

        try {

            verifier.verify(bodyString, webhookId, webhookSignature, webhookTimestamp);

            log.info("[PORTONE_WEBHOOK] 검증 성공: id={}", webhookId);


            PortoneWebhookPayload payload = objectMapper.readValue(rawBody, PortoneWebhookPayload.class);


            paymentService.processWebhook(payload, webhookId);

            return ResponseEntity.ok().build();

        } catch (WebhookVerificationException e) {
            log.warn("[PORTONE_WEBHOOK] 시그니처 검증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("[PORTONE_WEBHOOK] 처리 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}