package org.example.commercepayment.domain.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.commercepayment.domain.product.dto.ProductPageResponse;
import org.example.commercepayment.domain.product.dto.ProductResponse;
import org.example.commercepayment.domain.product.dto.ProductSearchRequest;
import org.example.commercepayment.domain.product.service.ProductService;
import org.example.commercepayment.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProductPageResponse>> list(@Valid @ModelAttribute ProductSearchRequest request) {
        ProductPageResponse response = productService.getProducts(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.findById(id)));
    }
}
