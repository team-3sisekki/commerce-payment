package org.example.commercepayment.domain.product.service;

import lombok.RequiredArgsConstructor;
import org.example.commercepayment.domain.product.dto.ProductPageResponse;
import org.example.commercepayment.domain.product.dto.ProductResponse;
import org.example.commercepayment.domain.product.dto.ProductSearchRequest;
import org.example.commercepayment.domain.product.entity.Product;
import org.example.commercepayment.domain.product.repository.ProductRepository;
import org.example.commercepayment.global.error.BusinessException;
import org.example.commercepayment.global.error.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductPageResponse getProducts(ProductSearchRequest request, Pageable pageable) {
        Page<Product> productPage = productRepository.findProductsByConditions(
                request.category(), request.minPrice(), request.maxPrice(),
                request.salesStatus(), request.isSoldOut(), pageable
        );

        return ProductPageResponse.of(productPage, pageable);
    }

    public ProductResponse findById(Long id) {
        return ProductResponse.from(findProductEntity(id));
    }

    public Product findProductEntity(Long id) {
        return productRepository.findById(id).orElseThrow(
                () -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
        );
    }

}