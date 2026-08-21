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
        // 1. 해당 회원이 똑같은 상품을 이미 장바구니에 담아두었는지 DB에서 조회합니다.
        // Optional은 결과가 '있을 수도 있고, 없을 수도 있음'을 안전하게 다루기 위한 자바 문법입니다.
        Optional<CartItem> existing = cartItemRepository.findByCart_Member_IdAndProduct_Id(
                cartItem.getMemberId(), cartItem.getProductId()
        );

        if (existing.isPresent()) { // 2-A. 이미 장바구니에 똑같은 상품이 존재하는 경우
            CartItem found = existing.get(); // 기존 데이터를 꺼냅니다.

            // 핵심 비즈니스 로직 (검증): 기존에 담은 수량 + 새로 담을 수량이 현재 남아있는 재고보다 많은지 확인!
            if (found.getProduct().getStock() < found.getQuantity() + cartItem.getQuantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }

            // 검증을 통과했다면 기존 데이터에 수량만 더해줍니다.
            // (JPA의 '더티 체킹(Dirty Checking)' 덕분에 DB에 따로 save를 안 해도 자동으로 업데이트됩니다.)
            found.addQuantity(cartItem.getQuantity());
            return found.getId();

        } else { // 2-B. 장바구니에 없는 완전히 새로운 상품을 담는 경우

            // 핵심 비즈니스 로직 (검증): 새로 담으려는 수량이 현재 재고보다 많은지 확인!
            if (cartItem.getProduct().getStock() < cartItem.getQuantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }

            // 검증을 통과했다면 DB에 새롭게 Insert(저장) 합니다.
            return cartItemRepository.save(cartItem).getId();
        }
    }

    // 장바구니 수량 변경: 이미 담긴 특정 상품의 수량을 수정합니다.
    @Transactional
    public void updateQuantity(Long memberId, Long itemId, int quantity) {
        // 1. 변경하려는 장바구니 항목(itemId)을 DB에서 찾습니다.
        CartItem item = cartItemRepository.findById(itemId)
                // 2. filter: 찾은 항목의 주인(memberId)이 지금 요청한 회원과 일치하는지(내 장바구니가 맞는지) 보안 검증합니다.
                .filter(ci -> ci.getMemberId().equals(memberId))
                // 3. 없거나 내 것이 아니라면 에러를 던집니다.
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        // 4. 최종적으로 변경하려는 수량이 현재 재고를 초과하는지 검증합니다.
        if (item.getProduct().getStock() < quantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }

        // 5. 수량을 변경합니다. (이 역시 더티 체킹으로 인해 DB 자동 반영됨)
        item.changeQuantity(quantity);
    }

    // 장바구니 단건 삭제: X 버튼 등을 눌러 특정 상품 1개를 장바구니에서 뺍니다.
    @Transactional
    public void removeItem(Long memberId, Long itemId) {
        // DB에서 항목 ID와 회원 ID가 모두 일치하는 데이터만 지웁니다.
        // 반환값(deleted)은 '실제로 삭제된 데이터의 개수'입니다.
        int deleted = cartItemRepository.deleteByIdAndMember_Id(itemId, memberId);

        // 지워진 데이터가 0개라면, 이미 없거나 내 장바구니 항목이 아니라는 뜻이므로 에러를 던집니다.
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
        // 회원의 ID를 기준으로 묶여있는 장바구니 항목들을 일괄 삭제합니다.
        cartItemRepository.deleteAllByMember_Id(memberId);
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