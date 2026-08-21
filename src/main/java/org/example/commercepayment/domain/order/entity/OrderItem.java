package org.example.commercepayment.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.commercepayment.domain.product.entity.Product;
import org.example.commercepayment.global.entity.BaseTimeEntity;

// 주문 상품. 주문 시점의 상품명·가격을 스냅샷으로 들고 있다.
// 상품이 나중에 이름/가격이 바뀌거나 단종돼도 주문 내역은 당시 그대로 보여야 하기 때문이다.
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 재고 복구·상품 상세 이동에 필요해서 원본 상품 참조도 유지한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 상품명 스냅샷
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    // 주문 시점 가격 스냅샷
    @Column(name = "order_price", nullable = false)
    private int orderPrice;

    @Column(nullable = false)
    private int quantity;

    @Builder
    private OrderItem(Product product, int orderPrice, int quantity) {
        this.product = product;
        this.productName = product.getName();   // 여기서 이름을 복사해 스냅샷으로 굳힌다
        this.orderPrice = orderPrice;
        this.quantity = quantity;
    }

    // package-private. Order.addOrderItem()에서만 호출하는 양방향 연관관계 설정용.
    void setOrder(Order order) {
        this.order = order;
    }

    // 이 항목의 소계. 주문 총액은 이 값들의 합이다.
    public int getSubtotal() {
        return orderPrice * quantity;
    }
}
