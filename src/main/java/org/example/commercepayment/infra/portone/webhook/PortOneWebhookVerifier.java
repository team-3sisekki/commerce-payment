package org.example.commercepayment.infra.portone.webhook;

import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.webhook.Webhook;
import io.portone.sdk.server.webhook.WebhookVerifier;
import org.example.commercepayment.infra.portone.config.PortOneProperties;
import org.springframework.stereotype.Component;

@Component
public class PortOneWebhookVerifier {

    private final WebhookVerifier webhookVerifier;

    public PortOneWebhookVerifier(PortOneProperties properties) {
        this.webhookVerifier = new WebhookVerifier(properties.getWebhookSecret());
    }

    /**
     * 웹훅 메시지를 검증하고 파싱된 {@link Webhook} 객체를 반환한다.
     *
     * @throws WebhookVerificationException 시그니처·타임스탬프 검증 실패 시
     */
    public Webhook verify(String body, String webhookId, String signature, String timestamp) throws WebhookVerificationException {
        return webhookVerifier.verify(body, webhookId, signature, timestamp);
    }
}
    