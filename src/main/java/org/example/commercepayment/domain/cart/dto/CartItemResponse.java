package org.example.commercepayment.domain.cart.dto;

import org.example.commercepayment.domain.cart.entity.CartItem;

public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        int price,
        int quantity,
        int stock,
        int itemTotalPrice // 추가: (price * quantity) 이 상품의 총 금액
) {
    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getPrice(),
                item.getQuantity(),
                item.getProduct().getStock(),
                item.getProduct().getPrice() * item.getQuantity()
        );
    }
}
