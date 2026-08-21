package org.example.commercepayment.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.commercepayment.domain.member.entity.Member;
import org.example.commercepayment.global.entity.BaseTimeEntity;
import org.example.commercepayment.global.error.BusinessException;
import org.example.commercepayment.global.error.ErrorCode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// 주문. 생성 후에는 수정·삭제하지 않고, 상태 변경만 결제·환불 흐름으로 발생한다.
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 노출용 주문번호 (DB PK와 분리). PK를 그대로 노출하면 주문량이 추측되고 남의 주문 ID도 찍기 쉽다.
    @Column(name = "order_number", nullable = false, unique = true, length = 100)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 주문 총액 (DDL 컬럼명: total_amount)
    @Column(name = "total_amount", nullable = false)
    private int totalAmount;

    // 사용 포인트(스냅샷_주문시). 실제 차감은 결제 확정 시점이고 여기서는 기록
    @Column(name = "used_point", nullable = false)
    private int usedPoint;

    // 신규 생성은 결제 대기
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PAYMENT_PENDING;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Builder
    private Order(Member member, List<OrderItem> orderItems, int usedPoint) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }
        if (usedPoint < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        this.member = member;
        this.usedPoint = usedPoint;
        this.orderNumber = generateOrderNumber();
        orderItems.forEach(this::addOrderItem);
        // 합계를 직접 계산. 외부에서 받으면 금액이 달라도 검증할 방법이 없음.
        this.totalAmount = this.orderItems.stream()
                .mapToInt(OrderItem::getSubtotal)
                .sum();
    }

    // 상태 변경의 통로(세터 생성 X)
    // 결제 확정이 중복되어도 여기서 막는다.
    // 재고 중복 복구도 막는다.
    public void transitTo(OrderStatus target) {
        if (!this.status.canTransitTo(target)) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = target;
    }


    // PG 실결제 금액. 0이면 PG 호출 생략
    public int getPgAmount() {
        return this.totalAmount - this.usedPoint;
    }

    public Long getMemberId() {
        return member.getId();
    }

    // 목록에 표시할 이름 ex) 사과 외 2건
    public String getOrderName() {
        if (orderItems.isEmpty()) {
            return "주문";
        }
        // 주문목록에 첫번째 상품이름 가져오기
        String firstName = orderItems.get(0).getProductName();
        // 하나면 그냥 출력
        if (orderItems.size() == 1) {
            return firstName;
        }
        // 여러개인 경우 나머지는 외로 처리
        return firstName + " 외 " + (orderItems.size() - 1) + "건";
    }

    // private로 해서 주문 생성 후 수정 불가
    private void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    // ex) ORD-202608140-A3W5R1C6
    private static String generateOrderNumber() {
        return "ORD-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // 주문 수량의 총합_부분 환불용?
    public int getTotalQuantity() {
        return orderItems.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }
}

