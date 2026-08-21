package org.example.commercepayment.domain.cart.repository;

import org.example.commercepayment.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.product WHERE ci.cart.member.id = :memberId")
    List<CartItem> findByMemberId(@Param("memberId") Long memberId);

    // Spring Data JPA 명명 규칙에 따라 경로 변경
    Optional<CartItem> findByCart_Member_IdAndProduct_Id(Long memberId, Long productId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.id = :id AND ci.cart.member.id = :memberId")
    int deleteByIdAndMember_Id(@Param("id") Long id, @Param("memberId") Long memberId);

    // 주문 생성 완료 직후 "주문한 장바구니 아이템만" 일괄 삭제
    // - member.id 조건: 남의 cartItemId를 섞어 보내도 삭제되지 않게 하는 소유권 검증
    // - IN절 일괄 삭제: 개별 deleteByIdAndMember_Id를 주문한 아이템 수만큼 반복 호출하는 대신 한 번의 쿼리로 처리 (N번 쿼리 → 1번)
    // - 반환 int: 실제로 삭제된 행 수
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.member.id = :memberId")
    void deleteAllByMember_Id(@Param("memberId") Long memberId);

    @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.product WHERE ci.id IN :ids AND ci.cart.member.id = :memberId")
    List<CartItem> findByIdInAndMember_IdWithProduct(@Param("ids") List<Long> ids, @Param("memberId") Long memberId);

    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.id IN :ids AND c.cart.member.id = :memberId")
    int deleteAllByIdInAndMemberId(@Param("ids") List<Long> ids, @Param("memberId") Long memberId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM CartItem ci WHERE ci.cart.member.id = :memberId AND ci.product.id IN :productIds")
    int deleteAllByMemberIdAndProductIdIn(@Param("memberId") Long memberId, @Param("productIds") List<Long> productIds);
}
