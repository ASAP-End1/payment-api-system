package com.bootcamp.paymentdemo.webhook.repository;

import com.bootcamp.paymentdemo.webhook.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookRepository extends JpaRepository<WebhookEvent, Long> {

    boolean existsByWebhookId(String webhookId);

}