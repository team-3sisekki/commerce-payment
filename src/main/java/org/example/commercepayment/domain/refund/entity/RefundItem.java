package org.example.commercepayment.domain.refund.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.commercepayment.domain.order.entity.OrderItem;
import org.example.commercepayment.global.entity.BaseTimeEntity;

@Entity
@Getter
@Table(name = "refund_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_id", nullable = false)
    private Refund refund;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(name = "refund_quantity", nullable = false)
    private int refundQuantity;

    @Column(name = "point_refund_amount", nullable = false)
    private int pointRefundAmount;

    @Column(name = "pg_refund_amount", nullable = false)
    private int pgRefundAmount;

    // 생성자 및 빌더는 private으로 캡슐화
    @Builder(access = AccessLevel.PRIVATE)
    private RefundItem(OrderItem orderItem, int refundQuantity, int pointRefundAmount, int pgRefundAmount) {
        this.orderItem = orderItem;
        this.refundQuantity = refundQuantity;
        this.pointRefundAmount = pointRefundAmount;
        this.pgRefundAmount = pgRefundAmount;
    }

    // 정적 팩토리 메서드를 통해서만 객체 생성
    public static RefundItem create(OrderItem orderItem, int refundQuantity, int pointRefundAmount, int pgRefundAmount) {
        if (refundQuantity <= 0) {
            throw new IllegalArgumentException("환불 수량은 1개 이상이어야 합니다.");
        }
        
        return RefundItem.builder()
                .orderItem(orderItem)
                .refundQuantity(refundQuantity)
                .pointRefundAmount(pointRefundAmount)
                .pgRefundAmount(pgRefundAmount)
                .build();
    }

    // 연관관계 편의 메서드 (Refund 엔티티에서 호출)
    void assignRefund(Refund refund) {
        this.refund = refund;
    }
}
