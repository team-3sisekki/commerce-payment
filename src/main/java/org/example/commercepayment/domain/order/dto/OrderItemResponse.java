package org.example.commercepayment.domain.order.dto;

import org.example.commercepayment.domain.order.entity.OrderItem;

public record OrderItemResponse(
        Long orderItemId,
        Long productId,
        String productName,
        int orderPrice,
        int quantity,
        int subtotal)
{
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProductName(),
                item.getOrderPrice(),
                item.getQuantity(),
                item.getSubtotal());
    }
}
