package org.example.commercepayment.domain.refund.component;

import org.example.commercepayment.domain.order.entity.OrderItem;
import org.example.commercepayment.domain.payment.entity.Payment;
import org.example.commercepayment.domain.refund.dto.RefundRequest;
import org.example.commercepayment.domain.refund.dto.RefundRequest.RefundItemRequest;
import org.example.commercepayment.domain.refund.entity.RefundItem;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RefundCalculator {

    // 계산 결과를 담아서 한 번에 돌려주는 전용 DTO
    public record RefundCalculationResult(
            List<RefundItem> refundItems,
            int totalPgRefundAmount,
            int totalPointRefundAmount,
            int pointRecoveryAmount
    ) {}

    // 서비스가 계산을 위임할 때 호출하는 메인 메서드! 복잡한 금액 계산 처리
    public RefundCalculationResult calculate(
            RefundRequest request,
            Map<Long, OrderItem> orderItemMap,
            Payment payment,
            boolean isFullRefund,
            int refundedPgAmount,
            int refundedPointAmount
    ) {
        int totalPgRefundAmount = 0;
        int totalPointRefundAmount = 0;

        if (isFullRefund) {
            // [분기 A: 전액/마지막 환불]
            // 비율로 쪼개면 1원 단위 오차가 생길 수 있으므로, '원래 결제 총액'에서 '기존에 환불받은 총액'을 통째로 뺀다.
            totalPgRefundAmount = payment.getPgAmount() - refundedPgAmount;
            totalPointRefundAmount = payment.getPointUsedAmount() - refundedPointAmount;
            log.info("전액/마지막 환불 금액 배정: PG={}, Point={}", totalPgRefundAmount, totalPointRefundAmount);
        }

        // 요청 전체를 RefundItem 리스트로 변환
        List<RefundItem> generatedRefundItems = buildRefundItems(request, orderItemMap, payment, isFullRefund, totalPointRefundAmount, totalPgRefundAmount);

        // 부분 환불일 경우, 개별 계산된 금액의 합산을 총액으로 갱신
        if (!isFullRefund) {
            totalPointRefundAmount = generatedRefundItems.stream().mapToInt(RefundItem::getPointRefundAmount).sum();
            totalPgRefundAmount = generatedRefundItems.stream().mapToInt(RefundItem::getPgRefundAmount).sum();
            log.info("부분 환불 개별 항목 합산 금액: PG={}, Point={}", totalPgRefundAmount, totalPointRefundAmount);
        }

        // 적립 포인트(회수 대상):  결제 시 받은 전체 적립금 × (이번 PG 환불액 / 전체 PG 실결제액)
        int pointRecovery = 0;
        if (payment.getPgAmount() > 0) {
            pointRecovery = (int) Math.floor((double) payment.getEarnedPointAmount() * totalPgRefundAmount / payment.getPgAmount());
            log.info("회수될 적립 포인트 계산: 회수액={} (전체 적립금={}, 이번 환불PG={}, 전체 PG 실결제액={})", pointRecovery, payment.getEarnedPointAmount(), totalPgRefundAmount, payment.getPgAmount());
        }

        return new RefundCalculationResult(generatedRefundItems, totalPgRefundAmount, totalPointRefundAmount, pointRecovery);
    }

    /**
     * 요청 전체를 RefundItem 리스트로 변환 (전액 환불 시 마지막 잔액 몰아주기 처리)
     */
    private List<RefundItem> buildRefundItems(
            RefundRequest request,
            Map<Long, OrderItem> orderItemMap,
            Payment payment,
            boolean isFullRefund,
            int totalPointRefundAmount,
            int totalPgRefundAmount) {
        
        List<RefundItem> items = new ArrayList<>();
        List<RefundItemRequest> requestItems = request.items();

        if (isFullRefund) {
            // [분기 A 전액/마지막 환불] 마지막 항목에 남은 잔액을 전부 통째로 맞춤
            List<RefundItemRequest> subList = requestItems.subList(0, requestItems.size() - 1);
            int accumulatedPointItemRefund = 0;
            int accumulatedPgItemRefund = 0;

            for (RefundItemRequest itemReq : subList) {
                RefundItem refundItem = allocateByRatio(itemReq, orderItemMap, payment);
                accumulatedPointItemRefund += refundItem.getPointRefundAmount();
                accumulatedPgItemRefund += refundItem.getPgRefundAmount();
                items.add(refundItem);
            }

            // 마지막 아이템은 비율로 계산하지 않고 (구해둔 총 환불액 - 여태까지 담은 환불액)을 넣음
            RefundItemRequest lastReq = requestItems.get(requestItems.size() - 1);
            OrderItem lastOrderItem = orderItemMap.get(lastReq.orderItemId());

            int lastPointRefundAmount = totalPointRefundAmount - accumulatedPointItemRefund;
            int lastPgRefundAmount = totalPgRefundAmount - accumulatedPgItemRefund;

            items.add(RefundItem.builder()
                    .orderItem(lastOrderItem)
                    .refundQuantity(lastReq.requestQuantity())
                    .pointRefundAmount(lastPointRefundAmount)
                    .pgRefundAmount(lastPgRefundAmount)
                    .build());

        } else {
            // [분기 B 부분 환불]
            for (RefundItemRequest itemReq : requestItems) {
                items.add(allocateByRatio(itemReq, orderItemMap, payment));
            }
        }
        return items;
    }

    /**
     * 아이템 1개의 금액을 비율로 계산 (내림 처리 및 잔돈 가산)
     */
    private RefundItem allocateByRatio(RefundItemRequest itemReq, Map<Long, OrderItem> orderItemMap, Payment payment) {
        OrderItem orderItem = orderItemMap.get(itemReq.orderItemId());
        // 이번에 환불할 상품의 순수 금액 (상품 1개 가격 × 환불 수량)
        int itemTotal = orderItem.getOrderPrice() * itemReq.requestQuantity();
        int orderTotal = payment.getOrder().getTotalAmount();

        // 포인트와 PG 결제액을 '전체 결제 비율'에 맞춰 쪼갠다 (소수점 내림 처리)
        int itemPointRefundAmount = (int) Math.floor((double) itemTotal * payment.getPointUsedAmount() / orderTotal); // 포인트 환불액 = 총 환불 금액 × (결제.포인트 사용 금액 / 결제.주문 총액)
        int itemPgRefundAmount = (int) Math.floor((double) itemTotal * payment.getPgAmount() / orderTotal); // PG 환불액   = 총 환불 금액 × (결제.PG 실결제 금액  / 결제.주문 총액)

        // 내림 처리 때문에 증발해버린 잔돈(1~2원)이 있는지 찾아서 PG 환불액에 가산
        int lostAmount = itemTotal - (itemPointRefundAmount + itemPgRefundAmount);
        if (lostAmount > 0) {
            itemPgRefundAmount += lostAmount;
        }

        return RefundItem.builder()
                .orderItem(orderItem)
                .refundQuantity(itemReq.requestQuantity())
                .pointRefundAmount(itemPointRefundAmount)
                .pgRefundAmount(itemPgRefundAmount)
                .build();
    }
}
