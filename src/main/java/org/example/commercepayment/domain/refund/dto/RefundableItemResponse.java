package org.example.commercepayment.domain.refund.dto;

import org.example.commercepayment.domain.order.entity.OrderItem;

public record RefundableItemResponse(
        Long orderItemId,
        String productName,
        int orderPrice,
        int originalQuantity,
        int remainQuantity
) {
    public static RefundableItemResponse from(OrderItem item, int remainQuantity) {
        return new RefundableItemResponse(
                item.getId(),
                item.getProductName(),
                item.getOrderPrice(),
                item.getQuantity(),
                remainQuantity
        );
    }
}
