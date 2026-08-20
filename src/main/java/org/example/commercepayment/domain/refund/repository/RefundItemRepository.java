package org.example.commercepayment.domain.refund.repository;

import org.example.commercepayment.domain.refund.dto.RefundedQuantityDto;
import org.example.commercepayment.domain.refund.entity.RefundItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RefundItemRepository extends JpaRepository<RefundItem, Long> {

    @Query("SELECT COALESCE(SUM(ri.refundQuantity), 0) " +
            "FROM RefundItem ri " +
            "WHERE ri.refund.payment.id = :paymentId " +
            "AND ri.refund.status = 'COMPLETED'")
    int sumRefundedQuantityByPaymentId(@Param("paymentId") Long paymentId);

    /**
     * 여러 주문 상품에 대한 누적 환불 수량 일괄 조회
     */
    @Query("SELECT new org.example.commercepayment.domain.refund.dto.RefundedQuantityDto(ri.orderItem.id, SUM(ri.refundQuantity)) " +
           "FROM RefundItem ri " +
           "WHERE ri.orderItem.id IN :orderItemIds " +
           "AND ri.refund.status = 'COMPLETED' " +
           "GROUP BY ri.orderItem.id")
    List<RefundedQuantityDto> findRefundedQuantitiesByOrderItemIds(@Param("orderItemIds") List<Long> orderItemIds);
}
