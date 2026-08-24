package org.example.commercepayment.domain.payment.repository;

import org.example.commercepayment.domain.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 주문 단건 조회 화면 - 결제 ID만
    @Query("SELECT p.id FROM Payment p WHERE p.order.id = :orderId")
    Optional<Long> findIdByOrderId(@Param("orderId") Long orderId);

    // 주문 목록 조회 - N+1 방지
    @Query("""
        SELECT p.order.id, p.id
        FROM Payment p
        WHERE p.order.id IN :orderIds
    """)
    List<Object[]> findIdsByOrderIds(@Param("orderIds") List<Long> orderIds);

    // Webhook에서 받아온 portonePaymentId 조건으로 Payment 조회 시 연관된 Order를 fetch join 으로 함께 로딩
    @Query("SELECT p FROM Payment p JOIN FETCH p.order WHERE p.portonePaymentId = :portonePaymentId")
    Optional<Payment> findByPortonePaymentId(@Param("portonePaymentId") String portonePaymentId);

    // 결제 확정 - orderId 기준 조회 (Order fetch join)
    @Query("SELECT p FROM Payment p JOIN FETCH p.order WHERE p.order.id = :orderId")
    Optional<Payment> findByOrderIdWithOrder(@Param("orderId") Long orderId);

    // 결제 상세 조회 - paymentId 기준 (Order fetch join)
    @Query("SELECT p FROM Payment p JOIN FETCH p.order WHERE p.id = :paymentId")
    Optional<Payment> findByIdWithOrder(@Param("paymentId") Long paymentId);

    // 결제 상세 조회 - paymentId 기준 (Order, OrderItems fetch join)
    @Query("SELECT p FROM Payment p JOIN FETCH p.order o JOIN FETCH o.orderItems WHERE p.id = :paymentId")
    Optional<Payment> findByIdWithOrderAndItems(@Param("paymentId") Long paymentId);

    @Query("SELECT p FROM Payment p JOIN FETCH p.order WHERE p.order.id IN :orderIds")
    List<Payment> findByOrderIdIn(@Param("orderIds") List<Long> orderIds);

    // 환불 처리 시 결제건(Payment)에만 단일 비관적 락을 획득하여 이중 환불을 방지합니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :paymentId")
    Optional<Payment> findByIdForRefundLockOnly(@Param("paymentId") Long paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT p FROM Payment p
    JOIN FETCH p.order
    WHERE p.order.id = :orderId
""")
    Optional<Payment> findByOrderIdWithOrderForUpdate(
            @Param("orderId") Long orderId
    );
}
