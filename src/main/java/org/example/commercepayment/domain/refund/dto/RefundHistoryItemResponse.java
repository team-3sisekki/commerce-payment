package org.example.commercepayment.domain.refund.dto;

import org.example.commercepayment.domain.refund.entity.RefundItem;

public record RefundHistoryItemResponse(
        Long refundItemId,
        String productName,
        int refundQuantity,
        int itemRefundAmount
) {
    public static RefundHistoryItemResponse from(RefundItem item) {
        return new RefundHistoryItemResponse(
                item.getId(),
                item.getOrderItem().getProduct().getName(),
                item.getRefundQuantity(),
                item.getPgRefundAmount() + item.getPointRefundAmount()
        );
    }
}
