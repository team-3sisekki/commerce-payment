package org.example.commercepayment.domain.payment.repository;

import org.example.commercepayment.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @Query("SELECT p FROM Payment p JOIN FETCH p.order WHERE p.order.id IN :orderIds")
    List<Payment> findByOrderIdIn(@Param("orderIds") List<Long> orderIds);
    
}
