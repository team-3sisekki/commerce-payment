package org.example.commercepayment.infra.portone.webhook;

import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.webhook.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commercepayment.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final PortOneWebhookVerifier portOneWebhookVerifier;
    private final WebhookHandler webhookHandler;
    private final WebhookEventService webhookEventService;

    @PostMapping("/portone")
    public ResponseEntity<ApiResponse<Void>> handlePortOneWebhook(
            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-timestamp") String webhookTimestamp,
            @RequestHeader("webhook-signature") String webhookSignature,
            @RequestBody String body) {

        log.info("[Webhook] received id={} timestamp={}", webhookId, webhookTimestamp);

        // 1. 시그니처 검증 : 실패 시 200 + 경고 로그 (standard-webhooks 권고)
        Webhook webhook;
        try {
            webhook = portOneWebhookVerifier.verify(body, webhookId, webhookSignature, webhookTimestamp);
        } catch (WebhookVerificationException e) {
            log.warn("[Webhook] verification failed id={} reason={}", webhookId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.ok());
        }

        // 2. 검증 통과 : 핸들러로 위임
        webhookHandler.handle(webhookId, webhook, body);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WebhookEvent>>> getWebhookEvents() {
        return ResponseEntity.ok(ApiResponse.ok(webhookEventService.getWebhookEvents()));
    }

}