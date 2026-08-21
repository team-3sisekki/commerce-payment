package org.example.commercepayment.domain.product.dto;

import org.example.commercepayment.domain.product.entity.Product;
import org.springframework.data.domain.Page;

import java.util.List;

// totalCount와 현재 페이지 메타를 포함하기 위해 응답 전용 DTO 생성

public record ProductPageResponse(
        List<ProductResponse> data,
        PageInfo pageInfo
) {
    public record PageInfo(
            int currentPage,
            int size,
            long totalElements,
            int totalPages
    ) {}

    public static ProductPageResponse of(Page<Product> productPage, int page, int size) {
        List<ProductResponse> data = productPage.getContent().stream()
                .map(ProductResponse::from)
                .toList();

        PageInfo pageInfo = new PageInfo(page, size, productPage.getTotalElements(), productPage.getTotalPages());

        return new ProductPageResponse(data, pageInfo);
    }
}
