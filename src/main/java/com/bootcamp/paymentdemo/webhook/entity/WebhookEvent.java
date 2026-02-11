package com.bootcamp.paymentdemo.webhook.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String webhookId;

    private String paymentId;
    private String eventStatus;

    @Enumerated(EnumType.STRING)
    private WebhookStatus status;

    private LocalDateTime receivedAt;

    @Builder
    public WebhookEvent(String webhookId, String paymentId, String eventStatus) {
        this.webhookId = webhookId;
        this.paymentId = paymentId;
        this.eventStatus = eventStatus;
        this.status = WebhookStatus.RECEIVED;
        this.receivedAt = LocalDateTime.now();
    }
}