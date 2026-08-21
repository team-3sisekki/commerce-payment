package org.example.commercepayment.domain.refund.dto;

public record RefundedQuantityDto(
        Long orderItemId,
        Long refundedQuantity
) {}