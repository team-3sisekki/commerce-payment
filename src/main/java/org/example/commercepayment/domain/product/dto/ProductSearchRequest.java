package org.example.commercepayment.domain.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// 파라미터 검증은 DTO로 묶어서 수정

public record ProductSearchRequest(
        String category,
        Integer minPrice,
        Integer maxPrice,
        String salesStatus,
        Boolean isSoldOut,
        @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.") Integer page,
        @Max(value = 100, message = "페이지 크기는 100 이하이어야 합니다.") Integer size,
        String sort
) {
    public ProductSearchRequest {
        if (page == null) page = 1;
        if (size == null) size = 20;
        if (sort == null || sort.isBlank()) sort = "LATEST";
    }
}
