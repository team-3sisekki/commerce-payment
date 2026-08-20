package org.example.commercepayment.domain.refund.repository;

import org.example.commercepayment.domain.refund.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    /**
     * 환불 상세
     */
    @Query("SELECT r FROM Refund r JOIN FETCH r.refundItems WHERE r.id = :id")
    Optional<Refund> findByIdWithItems(@Param("id") Long id);

    /**
     * 특정 결제 건에 대한 전체 환불 내역
     */
    List<Refund> findByPaymentId(Long paymentId);

    /**
     * 특정 결제 건에 대해 이미 완료된 총 PG 환불 금액
     */
    @Query("SELECT COALESCE(SUM(r.pgRefundAmount), 0) " +
            "FROM Refund r " +
            "WHERE r.payment.id = :paymentId " +
            "AND r.status = 'COMPLETED'")
    int sumRefundedPgAmountByPaymentId(@Param("paymentId") Long paymentId);
    
    /**
     * 특정 결제 건에 대해 이미 완료된 총 포인트 환불 금액
     */
    @Query("SELECT COALESCE(SUM(r.pointRefundAmount), 0) FROM Refund r WHERE r.payment.id = :paymentId AND r.status = 'COMPLETED'")
    int sumRefundedPointAmountByPaymentId(@Param("paymentId") Long paymentId);
}
