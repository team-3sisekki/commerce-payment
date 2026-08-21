package org.example.commercepayment.domain.refund.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.commercepayment.domain.payment.entity.Payment;
import org.example.commercepayment.global.entity.BaseTimeEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "refunds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "cancel_reason", nullable = false)
    private String cancelReason;

    @Column(name = "point_refund_amount", nullable = false)
    private int pointRefundAmount;

    @Column(name = "pg_refund_amount", nullable = false)
    private int pgRefundAmount;

    @Column(name = "point_recovery_amount", nullable = false)
    private int pointRecoveryAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefundStatus status;

    //  영속성 전이 (Cascade) 설정 : refund에 일어나는 모든 상태 변화 (저장, 삭제)를 refundItem에도 전파
    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefundItem> refundItems = new ArrayList<>();

    // 빌더 패턴 적용
    @Builder
    private Refund(Payment payment, String cancelReason, int pointRefundAmount, int pgRefundAmount, int pointRecoveryAmount) {
        this.payment = payment;
        this.cancelReason = cancelReason;
        this.pointRefundAmount = pointRefundAmount;
        this.pgRefundAmount = pgRefundAmount;
        this.pointRecoveryAmount = pointRecoveryAmount;
        this.status = RefundStatus.COMPLETED; // 기본 생성 상태값
    }

    // 비즈니스 로직 캡슐화 (상태 변경 - Enum에 위임)
    public void changeStatus(RefundStatus newStatus) {
        // Enum 내부에 정의된 상태 전이 검증 로직 호출
        this.status.validateTransitionTo(newStatus);
        this.status = newStatus;
    }

    public void addRefundItem(RefundItem item) {
        this.refundItems.add(item);
        item.assignRefund(this);
    }
}
