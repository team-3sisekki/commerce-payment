package org.example.commercepayment.domain.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.commercepayment.global.entity.BaseTimeEntity;
import org.example.commercepayment.global.error.BusinessException;
import org.example.commercepayment.global.error.ErrorCode;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, columnDefinition = "int UNSIGNED")
    private int price;

    @Column(name = "stock_quantity", nullable = false, columnDefinition = "int UNSIGNED DEFAULT 0")
    private int stock = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    // 추가된 필드: 카테고리
    @Column(nullable = false, length = 100)
    private String category;

    // 추가된 필드: 판매 상태 (ON_SALE, SOLD_OUT, DISCONTINUED)
    @Column(nullable = false, length = 30)
    private String salesStatus;

    @Builder
    private Product(String name, int price, int stock, String description, String category, String salesStatus) {
        if (price < 0) {
            throw new BusinessException(ErrorCode.INVALID_PRICE);
        }
        if (stock < 0) {
            throw new BusinessException(ErrorCode.INVALID_STOCK);
        }
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.description = description;
        this.category = category;
        this.salesStatus = salesStatus;
    }

    // 판매 중(ON_SALE)이 아닌 상품(품절/단종)은 장바구니에 담을 수 없도록 검증
    public void validateOnSale() {
        if (!"ON_SALE".equals(this.salesStatus)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_ON_SALE);
        }
    }

    // 비즈니스 로직: 재고 변경 시 자동으로 상태 업데이트
    public void deductStock(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }
        if (quantity > this.stock) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
        this.stock -= quantity;

        // 재고가 0이 되면 자동으로 품절 상태로 변경
        if (this.stock == 0 && "ON_SALE".equals(this.salesStatus)) {
            this.salesStatus = "SOLD_OUT";
        }
    }

    // 주문 취소 시 선차감된 재고 되돌리는 메서드
    public void restoreStock(int quantity) {
        if (quantity <= 0)  {   // 음수와 0 입력을 방지하는 조건 문
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }
        this.stock += quantity;

        // (선택) 재고가 복구되면 다시 판매중으로 변경할지 여부 결정
        if (this.stock > 0 && "SOLD_OUT".equals(this.salesStatus)) {
            this.salesStatus = "ON_SALE";
        }
    }
}