package org.example.commercepayment.domain.cart.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.commercepayment.domain.member.entity.Member;
import org.example.commercepayment.global.entity.BaseTimeEntity;

@Entity
@Table(name = "carts") // 기존 DB의 장바구니 테이블명과 맞춰주세요.
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1명의 회원은 1개의 장바구니를 가집니다.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Builder
    private Cart(Member member) {
        this.member = member;
    }
}