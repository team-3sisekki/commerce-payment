package org.example.commercepayment.domain.cart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commercepayment.domain.cart.dto.CartResponse;
import org.example.commercepayment.domain.cart.entity.CartItem;
import org.example.commercepayment.domain.cart.repository.CartItemRepository;
import org.example.commercepayment.global.error.BusinessException;
import org.example.commercepayment.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartItemRepository cartItemRepository;

    // 장바구니 조회: 특정 회원의 장바구니 상품 목록과 총 합계 금액을 반환합니다.
    public CartResponse getCartItems(Long memberId) {
        return CartResponse.from(cartItemRepository.findByMemberId(memberId));
    }

    @Transactional
    public Long addItem(CartItem cartItem) {

        Optional<CartItem> existing = cartItemRepository.findByCart_Member_IdAndProduct_Id(
                cartItem.getMemberId(), cartItem.getProductId()
        );

        if (existing.isPresent()) {
            CartItem found = existing.get();
            found.getProduct().validateStockAvailable(found.getQuantity() + cartItem.getQuantity());
            found.addQuantity(cartItem.getQuantity());

            return found.getId();

        } else { // 2-B. 장바구니에 없는 완전히 새로운 상품을 담는 경우
            cartItem.getProduct().validateStockAvailable(cartItem.getQuantity());

            return cartItemRepository.save(cartItem).getId();
        }
    }

    // 장바구니 수량 변경: 이미 담긴 특정 상품의 수량을 수정합니다.
    @Transactional
    public void updateQuantity(Long memberId, Long itemId, int quantity) {

        CartItem item = cartItemRepository.findById(itemId)
                .filter(ci -> ci.getMemberId().equals(memberId))
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        item.getProduct().validateStockAvailable(quantity);

        item.changeQuantity(quantity);
    }

    // 장바구니 단건 삭제: X 버튼 등을 눌러 특정 상품 1개를 장바구니에서 뺍니다.
    @Transactional
    public void removeItem(Long memberId, Long itemId) {

        int deleted = cartItemRepository.deleteByIdAndMember_Id(itemId, memberId);

        if (deleted == 0) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
    }

    public List<CartItem> findCartEntities(Long memberId) {
        return cartItemRepository.findByMemberId(memberId);
    }

    // 장바구니 전체 비우기: 회원의 장바구니 데이터를 한 번에 모두 삭제합니다.
    @Transactional
    public void clearCart(Long memberId) {

        cartItemRepository.deleteAllByMember_Id(memberId);
    }

    public void deleteCartItemsByProductIds(Long memberId, List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        cartItemRepository.deleteAllByMemberIdAndProductIdIn(memberId, productIds);
    }
    // 주문 연동용 메서드 (주문 도메인에서 호출하여 사용)
    public List<CartItem> findCartEntitiesByIds(Long memberId, List<Long> cartItemIds) {
        return cartItemRepository.findByIdInAndMember_IdWithProduct(cartItemIds, memberId);
    }

    @Transactional
    public void clearCartItems(List<Long> orderedItemIds, Long memberId) {
        int deleted = cartItemRepository.deleteAllByIdInAndMemberId(orderedItemIds, memberId);
        if (deleted != orderedItemIds.size()) {
            log.warn("장바구니 삭제 불일치: expected={}, actual={}, memberId={}",
                    orderedItemIds.size(), deleted, memberId);
        }
    }
}