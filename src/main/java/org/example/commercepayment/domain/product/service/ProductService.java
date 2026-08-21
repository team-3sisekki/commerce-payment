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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    // findAll() 메서드를 제거 -> 페이징과 정렬을 처리하는 메서드 추가
    public ProductPageResponse getProducts(ProductSearchRequest request) {

        // 1. 정렬(Sort) 기준 설정 (기본값: 최신순)
        Sort sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("PRICE_ASC".equalsIgnoreCase(request.sort())) {
            sortObj = Sort.by(Sort.Direction.ASC, "price");
        } else if ("PRICE_DESC".equalsIgnoreCase(request.sort())) {
            sortObj = Sort.by(Sort.Direction.DESC, "price");
        }

        // 2. Pageable 객체 생성 (JPA는 페이지를 0부터 시작하므로 page - 1 처리)
        Pageable pageable = PageRequest.of(request.page() - 1, request.size(), sortObj);

        // 3. 조건에 맞는 데이터 조회
        Page<Product> productPage = productRepository.findProductsByConditions(
                request.category(), request.minPrice(), request.maxPrice(),
                request.salesStatus(), request.isSoldOut(), pageable
        );

        // 4. 엔티티를 DTO로 변환
        List<ProductResponse> data = productPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        // 5. 페이지 메타데이터 생성
        ProductPageResponse.PageInfo pageInfo = new ProductPageResponse.PageInfo(
                request.page(), request.size(), productPage.getTotalElements(), productPage.getTotalPages()
        );

        return new ProductPageResponse(data, pageInfo);
    }

//    public List<ProductResponse> findAll() {
//        return productRepository.findAll().stream()
//                .map(this::toResponse)
//                .toList();
//    }

    public ProductResponse findById(Long id) {
        Product product = findProductEntity(id);
        return toResponse(product);
    }

    public Product findProductEntity(Long id) {
        return productRepository.findById(id).orElseThrow(
                () -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
        );
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getDescription(),
                product.getCategory(),          // 추가
                product.getSalesStatus()        // 추가
        );
    }

}