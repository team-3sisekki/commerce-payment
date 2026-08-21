package org.example.commercepayment.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.commercepayment.domain.order.entity.Order;
import org.example.commercepayment.global.entity.BaseTimeEntity;
import org.example.commercepayment.global.error.BusinessException;
import org.example.commercepayment.global.error.ErrorCode;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "portone_payment_id", nullable = false, length = 200, unique = true)
    private String portonePaymentId;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "point_used_amount", nullable = false)
    private int pointUsedAmount;

    @Column(name = "pg_amount", nullable = false)
    private int pgAmount;

    @Column(name = "earned_point_amount", nullable = false)
    private int earnedPointAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "fail_reason", length = 50)
    private FailReason failReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Builder
    private Payment(Order order, int amount, int pointUsedAmount) {
        // 1. 음수 및 총액 초과 포인트 방지 검증 추가
        if (amount < 0 || pointUsedAmount < 0 || pointUsedAmount > amount) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        this.order = order;
        this.portonePaymentId = generatePortonePaymentId();
        this.amount = amount;
        this.pointUsedAmount = pointUsedAmount;
        this.pgAmount = amount - pointUsedAmount;
        this.earnedPointAmount = 0;
        this.status = PaymentStatus.PENDING;
    }

    private static String generatePortonePaymentId() {
        return "pay_" + UUID.randomUUID();
    }

    public void complete(int earnedPointAmount) {
        changeStatus(PaymentStatus.COMPLETED);
        this.earnedPointAmount = earnedPointAmount;
        this.paidAt = LocalDateTime.now();
    }

    public void fail(FailReason reason) {
        changeStatus(PaymentStatus.FAILED);
        this.failReason = reason;
    }

    public void cancel() {
        // 2. 중복 세팅 제거 (changeStatus 내부에서 처리함)
        changeStatus(PaymentStatus.CANCELLED);
    }
    
    public void fullRefund() {
        changeStatus(PaymentStatus.FULL_REFUND);
    }
    
    public void partialRefund() {
        changeStatus(PaymentStatus.PARTIAL_REFUND);
    }

    // 3. 전액 포인트 결제 여부 확인 메서드 추가
    public boolean isPointOnly() {
        return this.pgAmount == 0;
    }

    private void changeStatus(PaymentStatus target) {
        if (!this.status.canTransitTo(target)) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
        this.status = target;
    }
}
