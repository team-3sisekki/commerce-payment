package org.example.commercepayment.domain.refund.dto;

import org.example.commercepayment.domain.refund.entity.Refund;
import java.time.LocalDateTime;
import java.util.List;

public record RefundHistoryResponse(
        Long refundId,
        LocalDateTime refundDate,
        String status,
        int totalRefundAmount,
        int pgRefundAmount,
        int pointRefundAmount,
        int pointRecoveryAmount,
        List<RefundHistoryItemResponse> items
) {
    public static RefundHistoryResponse from(Refund refund) {
        return new RefundHistoryResponse(
                refund.getId(),
                refund.getCreatedAt(),
                refund.getStatus().name(),
                refund.getPgRefundAmount() + refund.getPointRefundAmount(),
                refund.getPgRefundAmount(),
                refund.getPointRefundAmount(),
                refund.getPointRecoveryAmount(),
                refund.getRefundItems().stream()
                        .map(RefundHistoryItemResponse::from)
                        .toList()
        );
    }
}
