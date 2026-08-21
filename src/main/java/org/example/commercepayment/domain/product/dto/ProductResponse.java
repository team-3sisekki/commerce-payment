package org.example.commercepayment.domain.product.dto;

import org.example.commercepayment.domain.product.entity.Product;

public record ProductResponse(

        Long id,
        String name,
        int price,
        int stock,
        String description,
        String category,      // 추가
        String salesStatus    // 추가
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getDescription(),
                product.getCategory(),
                product.getSalesStatus()
        );
    }
}
