package org.example.commercepayment.infra.portone.webhook;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    // 동일한 webhookId로 이미 처리된 웹훅이 있는지 확인 (멱등성 보장용)
    // PortOne이 네트워크 타임아웃 등으로 같은 이벤트를 재전송해도 중복 처리되지 않도록 사전 체크한다.
    // 실제 쿼리: SELECT we.id FROM webhook_events we WHERE we.webhook_id = ? LIMIT 1;
    boolean existsByWebhookId(String webhookId);

}