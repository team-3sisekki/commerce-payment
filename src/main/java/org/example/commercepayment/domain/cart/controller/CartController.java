package org.example.commercepayment.domain.cart.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.commercepayment.domain.cart.dto.AddCartRequest;
import org.example.commercepayment.domain.cart.dto.AddCartResponse;
import org.example.commercepayment.domain.cart.dto.CartResponse;
import org.example.commercepayment.domain.cart.dto.UpdateCartRequest;
import org.example.commercepayment.domain.cart.facade.CartFacade;
import org.example.commercepayment.domain.cart.service.CartService;
import org.example.commercepayment.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor

public class CartController {

    private final CartFacade cartFacade;
    private final CartService cartService;

    // 응답 타입 변경 (List -> CartResponse)
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getItems(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.getCartItems(memberId)));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<AddCartResponse>> addItem(@AuthenticationPrincipal Long memberId,
                                                   @Valid @RequestBody AddCartRequest request) {
        Long cartItemId = cartFacade.addItem(memberId, request);
        return ResponseEntity.ok(ApiResponse.ok(new AddCartResponse(cartItemId)));
    }

    @PatchMapping("/items/{id}")
    public ResponseEntity<ApiResponse<Void>> updateQuantity(@AuthenticationPrincipal Long memberId,
                                               @PathVariable Long id,
                                               @Valid @RequestBody UpdateCartRequest request) {
        cartService.updateQuantity(memberId, id, request.quantity());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(@AuthenticationPrincipal Long memberId,
                                           @PathVariable Long itemId) {
        cartService.removeItem(memberId, itemId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // 추가: 장바구니 전체 비우기 API
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(@AuthenticationPrincipal Long memberId) {
        cartService.clearCart(memberId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}