package com.bootcamp.paymentdemo.webhook.service;

import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.webhook.Webhook;
import io.portone.sdk.server.webhook.WebhookVerifier;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PortOneSdkWebhookVerifier {

    @Value("${portone.webhook.secret}")
    private String webhookSecret;

    private WebhookVerifier webhookVerifier;

    @PostConstruct
    void init() {

        this.webhookVerifier = new WebhookVerifier(webhookSecret);
        log.info("포트원 웹훅 검증기 초기화, yml 파일내에 설정해둔 secret값으로 초기화 완료");
    }


    public Webhook verify(String msgBody, String msgId, String msgSignature, String msgTimestamp)
            throws WebhookVerificationException {
        return webhookVerifier.verify(msgBody, msgId, msgSignature, msgTimestamp);
    }
}