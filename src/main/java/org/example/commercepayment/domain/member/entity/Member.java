package org.example.commercepayment.domain.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.commercepayment.global.entity.BaseTimeEntity;

@Getter
@Entity
@Table(name="members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String email;

    @Column(nullable = false, length = 255)
    protected String password;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    // point 잔액. 환불로 적립분을 회수할 때 음수가 될 수 있으므로 UNSIGNED를 쓰지 않는다.
    @Column(nullable = false)
    private int point = 0; // 신규가입 시 0P로 시작

    @Builder
    public Member(String email, String password, String name, String phoneNumber) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    // 포인트 잔액 변경 메서드
    // Member는 얼마인지만 알고, 왜 바뀌는지는 PointService에서 처리
    public void addPoint(int signedAmount) {
        this.point += signedAmount;
    }
}
