package org.example.commercepayment.domain.product.dto;

public record ProductSearchRequest(
        String category,
        Integer minPrice,
        Integer maxPrice,
        String salesStatus,
        Boolean isSoldOut
) {
}
