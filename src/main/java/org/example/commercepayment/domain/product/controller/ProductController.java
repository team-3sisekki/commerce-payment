package org.example.commercepayment.domain.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.commercepayment.domain.product.dto.ProductPageResponse;
import org.example.commercepayment.domain.product.dto.ProductResponse;
import org.example.commercepayment.domain.product.dto.ProductSearchRequest;
import org.example.commercepayment.domain.product.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ProductPageResponse> list(@Valid @ModelAttribute ProductSearchRequest request) {
        ProductPageResponse response = productService.getProducts(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }
}
