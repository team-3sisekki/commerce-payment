package org.example.commercepayment.domain.refund.repository;

import org.example.commercepayment.domain.refund.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    /**
     * 특정 결제 건에 대한 전체 환불 내역
     */
    @Query("SELECT DISTINCT r FROM Refund r " +
           "JOIN FETCH r.refundItems ri " +
           "JOIN FETCH ri.orderItem oi " +
           "JOIN FETCH oi.product p " +
           "WHERE r.payment.id = :paymentId " +
           "ORDER BY r.createdAt DESC")
    List<Refund> findByPaymentIdWithItems(@Param("paymentId") Long paymentId);

    /**
     * 특정 결제 건에 대해 이미 완료된 총 PG 환불 금액
     */
    @Query("SELECT COALESCE(SUM(r.pgRefundAmount), 0) " +
            "FROM Refund r " +
            "WHERE r.payment.id = :paymentId " +
            "AND r.status = 'COMPLETED'")
    int sumRefundedPgAmountByPaymentId(@Param("paymentId") Long paymentId);


}
