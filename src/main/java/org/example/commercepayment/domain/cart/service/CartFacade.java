package org.example.commercepayment.domain.cart.service;

import lombok.RequiredArgsConstructor;
import org.example.commercepayment.domain.cart.dto.AddCartRequest;
import org.example.commercepayment.domain.cart.entity.Cart;
import org.example.commercepayment.domain.cart.entity.CartItem;
import org.example.commercepayment.domain.member.service.MemberService;
import org.example.commercepayment.domain.product.entity.Product;
import org.example.commercepayment.domain.product.service.ProductService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.example.commercepayment.domain.member.entity.Member;

@Component
@RequiredArgsConstructor
public class CartFacade {

    private final CartService cartService;
    private final MemberService memberService;
    private final ProductService productService;
    private final org.example.commercepayment.domain.cart.repository.CartRepository cartRepository; // 추가

    @Transactional
    public Long addItem(Long memberId, AddCartRequest request) {
        Member member = memberService.findById(memberId);
        Product product = productService.findProductEntity(request.productId());
        product.validateOnSale();

        // 회원의 장바구니를 찾거나 없으면 새로 생성합니다.
        Cart cart = cartRepository.findByMemberId(member.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().member(member).build()));

        CartItem cartItem = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(request.quantity())
                .build();
        return cartService.addItem(cartItem);
    }
}