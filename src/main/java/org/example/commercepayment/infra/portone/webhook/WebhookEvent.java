package org.example.commercepayment.infra.portone.webhook;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.commercepayment.global.entity.BaseTimeEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_events")
@Getter
@NoArgsConstructor
public class WebhookEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "webhook_id", nullable = false, unique = true, length = 200)
    private String webhookId;

    @Column(nullable = false, length = 100)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WebhookStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    public WebhookEvent(String webhookId, String type, String payload) {
        this.webhookId = webhookId;
        this.type = type;
        this.status = WebhookStatus.RECEIVED;
        this.payload = payload;
    }

    public void markAsProcessed() {
        this.status = WebhookStatus.PROCESSED;
        this.finishedAt = LocalDateTime.now();
        this.failureReason = null;
    }

    public void markAsIgnored(String reason) {
        this.status = WebhookStatus.IGNORED;
        this.finishedAt = LocalDateTime.now();
        this.failureReason = reason;
    }

    public void markAsFailed(String reason) {
        this.status = WebhookStatus.FAILED;
        this.finishedAt = LocalDateTime.now();
        this.failureReason = reason;
    }

}