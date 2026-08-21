package org.example.commercepayment.domain.order.dto;

import org.example.commercepayment.domain.cart.entity.CartItem;
import org.example.commercepayment.domain.product.entity.Product;

import java.util.List;

public record CheckoutResponse(
        List<CheckoutItemResponse> items,
        int totalPrice,
        int availablePoint)
{

    public static CheckoutResponse from(List<CartItem> cartItems, int availablePoint) {
        List<CheckoutItemResponse> items = cartItems.stream()
                .map(CheckoutItemResponse::from)
                .toList();

        int totalPrice = items.stream()
                .mapToInt(CheckoutItemResponse::subtotal)
                .sum();

        return new CheckoutResponse(items, totalPrice, availablePoint);
    }

    public record CheckoutItemResponse(
            Long productId,
            String productName,
            int price,
            int quantity,
            int subtotal,
            int stock,                 // 현재 재고
            boolean available)         // 주문 가능 여부 (재고 >= 수량)
    {
        public static CheckoutItemResponse from(CartItem cartItem) {
            Product product = cartItem.getProduct();
            int price = product.getPrice();
            int quantity = cartItem.getQuantity();

            return new CheckoutItemResponse(
                    product.getId(),
                    product.getName(),
                    price,
                    quantity,
                    price * quantity,
                    product.getStock(),
                    product.getStock() >= quantity   // 결제 화면에서 품절을 미리 안내
            );
        }
    }
}
