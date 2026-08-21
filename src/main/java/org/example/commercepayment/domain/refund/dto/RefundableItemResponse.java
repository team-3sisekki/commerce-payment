package org.example.commercepayment.domain.refund.dto;

public record RefundableItemResponse(
        Long orderItemId,
        String productName,
        int orderPrice,
        int originalQuantity,
        int remainQuantity
) {}
