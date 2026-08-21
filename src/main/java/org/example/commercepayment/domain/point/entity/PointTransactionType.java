package org.example.commercepayment.domain.point.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.commercepayment.global.error.BusinessException;
import org.example.commercepayment.global.error.ErrorCode;

// 포인트 거래 타입. 값 이름은 DDL의 transaction_type 컬럼 주석과 일치시킨다.
// 각 타입이 부호(+/-)를 갖고 있어 서비스에서 부호 실수를 막는다.
@Getter
@RequiredArgsConstructor
public enum PointTransactionType {

    USE("사용", -1),            // 결제 시 사용
    EARN("적립", 1),            // 결제 완료 시 적립
    USE_RESTORE("사용복구", 1),  // 환불 시 사용분 반환
    EARN_REVOKE("적립회수", -1); // 환불 시 적립분 회수

    private final String description;
    private final int sign;

    // 양수 금액을 타입에 맞는 부호로 변환. USE.applySign(3000) = -3000
    public int applySign(int positiveAmount) {
        if (positiveAmount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_POINT_AMOUNT, "amount=" + positiveAmount);
        }
        return sign * positiveAmount;
    }
}
