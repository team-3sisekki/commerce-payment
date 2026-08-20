package org.example.commercepayment.domain.refund.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commercepayment.domain.order.entity.OrderStatus;
import org.example.commercepayment.domain.payment.entity.PaymentStatus;
import org.example.commercepayment.domain.refund.dto.RefundRequest;
import org.example.commercepayment.domain.refund.dto.RefundRequest.RefundItemRequest;
import org.example.commercepayment.domain.refund.dto.RefundedQuantityDto;
import org.example.commercepayment.domain.refund.entity.Refund;
import org.example.commercepayment.domain.refund.entity.RefundItem;
import org.example.commercepayment.domain.refund.entity.RefundStatus;
import org.example.commercepayment.domain.refund.repository.RefundItemRepository;
import org.example.commercepayment.domain.refund.repository.RefundRepository;
import org.example.commercepayment.domain.order.entity.OrderItem;
import org.example.commercepayment.domain.order.repository.OrderItemRepository;
import org.example.commercepayment.domain.payment.entity.Payment;
import org.example.commercepayment.domain.payment.repository.PaymentRepository;
import org.example.commercepayment.domain.point.service.PointService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final RefundItemRepository refundItemRepository;
    private final org.example.commercepayment.domain.point.repository.PointTransactionRepository pointTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;
    private final PointService pointService;

    /**
     * 선검증 및 DB 갱신
     */
    @Transactional
    public Refund calculateAndSaveRefund(Long memberId, RefundRequest request) {
        log.info("========== [환불 처리 시작] ==========");
        log.info("요청 정보: 결제ID={}, 회원ID={}, 환불상품갯수={}", request.paymentId(), memberId, request.items().size());
        
        Payment payment = paymentRepository.findById(request.paymentId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제 건입니다."));

        /* 본인 소유 주문 여부 확인*/
        if (!payment.getOrder().getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 결제 건만 환불할 수 있습니다.");
        }

        /* 결제 상태(완료/부분환불) 확인*/
        if (payment.getStatus() != PaymentStatus.COMPLETED
            && payment.getStatus() != PaymentStatus.PARTIAL_REFUND) {
            throw new IllegalArgumentException("환불 가능한 결제 상태가 아닙니다.");
        }

        /* 환불 대상 상품의 잔여 환불 가능 수량 초과 여부 확인*/
        // 1. 환불하려는 상품들의 ID(번호)만 따로 모아서 리스트로 만든다.
        // 예: [상품A_ID, 상품B_ID, 상품C_ID]
        List<Long> itemIds = request.items().stream()
                            .map(RefundItemRequest::orderItemId)
                            .toList();

        // 2-1) 환불하려는 모든 상품의 '원본 정보(최초 주문 수량 등)'를 한 번에 조회 >  찾기 쉽게 Map(사전) 형태로 변경 (상품ID:상품 정보)
        Map<Long, OrderItem> orderItemMap = orderItemRepository.findAllById(itemIds).stream()
                                            .collect(toMap(OrderItem::getId, item -> item));

        // 2-2) 환불하려는 모든 상품의 '지금까지 이미 환불된 누적 수량'을 한 번에 조회 > 찾기 쉽게 Map 형태로 변경 (상품ID:누적 환불 수량)
        Map<Long, Long> refundedMap = refundItemRepository.findRefundedQuantitiesByOrderItemIds(itemIds).stream()
                                        .collect(toMap(
                                                RefundedQuantityDto::orderItemId,
                                                RefundedQuantityDto::refundedQuantity
                                        ));

        // 3.  잔여 수량 검증
        for (RefundItemRequest item : request.items()) {
            OrderItem orderItem = orderItemMap.get(item.orderItemId());
            if (orderItem == null) {
                throw new IllegalArgumentException("상품 ID " + item.orderItemId() + "는 존재하지 않는 주문 상품입니다.");
            }

            // 최초에 주문했던 수량
            int originalQuantity = orderItem.getQuantity();
            // 과거에 이미 환불했던 수량 (환불 이력이 아예 없으면 0개로 취급)
            int refundedQuantity = refundedMap.getOrDefault(item.orderItemId(), 0L).intValue();
            // 아직 환불받을 수 있는 진짜 남은 수량 = (최초 주문 수량 - 기존 환불 수량)
            int remainingQuantity = originalQuantity - refundedQuantity;

            // 이번에 환불해달라고 요청한 수량이, 남은 수량보다 많으면 에러
            if (item.requestQuantity() > remainingQuantity) {
                throw new IllegalArgumentException("상품 ID " + item.orderItemId() + "의 잔여 환불 가능 수량을 초과했습니다.");
            }
        }

        // TODO: 동일 환불에 대한 중복 요청 여부 확인

        // 이번에 사용자가 환불해 달라고 요청한 *'총 상품 개수'*
        int totalRequestedQuantity = request.items().stream()
                                    .mapToInt(RefundItemRequest::requestQuantity)
                                    .sum();

        // 결제 건에 속한 '주문헀던 총 개수'와 '기존에 이미 환불했던 개수'를 빼서 *'현재 남은 환불 가능 개수'* 계산
        int totalOriginalPaymentQuantity = payment.getOrder().getTotalQuantity();
        int totalRefundedPaymentQuantity = refundItemRepository.sumRefundedQuantityByPaymentId(request.paymentId());
        int paymentRemainingQuantity = totalOriginalPaymentQuantity - totalRefundedPaymentQuantity;

        // 최종적으로 돌려줄 포인트와 PG(카드 등) 금액
        int totalPointRefundAmount = 0;
        int totalPgRefundAmount = 0;

        // '총 상품 개수' - '현재 남은 환불 가능 개수' 로 전액/마지막 환불인지 부분환불 인지 분개처리
        boolean isFullRefund = (totalRequestedQuantity == paymentRemainingQuantity);
        log.info("전액환불여부={} (요청수량={}, 남은결제수량={})", isFullRefund, totalRequestedQuantity, paymentRemainingQuantity);

        if (isFullRefund) {
            // [분기 A: 전액/마지막 환불]
            // 비율로 쪼개면 1원 단위 오차가 생길 수 있으므로, '원래 결제 총액'에서 '기존에 환불받은 총액'을 통째로 뺀다.
            int refundedPgAmount = refundRepository.sumRefundedPgAmountByPaymentId(payment.getId()); // 이미 환불 완료된 금액
            int refundedPointAmount = refundRepository.sumRefundedPointAmountByPaymentId(payment.getId()); // 이미 환불 완료된 포인트

            totalPgRefundAmount = payment.getPgAmount() - refundedPgAmount;
            totalPointRefundAmount = payment.getPointUsedAmount() - refundedPointAmount;
            log.info("전액/마지막 환불 금액 배정: PG={}, Point={}", totalPgRefundAmount, totalPointRefundAmount);

            // 전액 환불이므로 결제와 주문 상태를 모두 '환불/취소' 상태로 변경
            payment.fullRefund();
            payment.getOrder().transitTo(OrderStatus.CANCELED);
        } else {
            // [분기 B: 일부 상품만 부분 환불]
            // 부분 환불이므로 결제 상태만 바꾸고, 주문 상태는 그대로 유지한다.
            payment.partialRefund();
        }

        // 요청 전체를 RefundItem 리스트로 변환
        List<RefundItem> generatedRefundItems = buildRefundItems(request, orderItemMap, payment, isFullRefund, totalPointRefundAmount, totalPgRefundAmount);

        // 부분 환불일 경우, 개별 계산된 금액의 합산을 총액으로 갱신
        if (!isFullRefund) {
            totalPointRefundAmount = generatedRefundItems.stream().mapToInt(RefundItem::getPointRefundAmount).sum();
            totalPgRefundAmount = generatedRefundItems.stream().mapToInt(RefundItem::getPgRefundAmount).sum();
            log.info("부분 환불 개별 항목 합산 금액: PG={}, Point={}", totalPgRefundAmount, totalPointRefundAmount);
        }

        // 환불된 상품 재고 수량 원복 (+)
        restoreStocks(request, orderItemMap);

        // 적립 포인트(회수 대상):  결제 시 받은 전체 적립금 × (이번 PG 환불액 / 전체 PG 실결제액)
        int pointRecovery = 0;
        if (payment.getPgAmount() > 0) {
            pointRecovery = (int) Math.floor((double) payment.getEarnedPointAmount() * totalPgRefundAmount / payment.getPgAmount());
            log.info("회수될 적립 포인트 계산: 회수액={} (전체 적립금={}, 이번 환불PG={}, 전체 PG 실결제액={})", pointRecovery, payment.getEarnedPointAmount(), totalPgRefundAmount, payment.getPgAmount());
        }

        // 사용 포인트를 복구해주고, 적립해준 포인트를 회수한다.
        if (totalPointRefundAmount > 0) {
            pointService.restoreUse(payment.getOrder().getMemberId(), payment, totalPointRefundAmount);
        }
        if (pointRecovery > 0) {
            pointService.revokeEarn(payment.getOrder().getMemberId(), payment, pointRecovery);
        }

        // 환불(Refund) 내역(부모) 생성
        Refund refund = Refund.create(
                payment,
                request.cancelReason(),
                totalPointRefundAmount,
                totalPgRefundAmount
        );

        Refund savedRefund = refundRepository.save(refund);

        // 환불 아이템(RefundItem) 내역(자식) 넣기
        // JPA 영속성 전이(Cascade)
        for (RefundItem refundItem : generatedRefundItems) {
            savedRefund.addRefundItem(refundItem);
        }

        return savedRefund;
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

            items.add(RefundItem.create(lastOrderItem, lastReq.requestQuantity(), lastPointRefundAmount, lastPgRefundAmount));

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
        int itemPointRefundAmount = (int) Math.floor((double) itemTotal * payment.getPointUsedAmount() / orderTotal);
        int itemPgRefundAmount = (int) Math.floor((double) itemTotal * payment.getPgAmount() / orderTotal);

        // 내림 처리 때문에 증발해버린 잔돈(1~2원)이 있는지 찾아서 PG 환불액에 가산
        int lostAmount = itemTotal - (itemPointRefundAmount + itemPgRefundAmount);
        if (lostAmount > 0) {
            itemPgRefundAmount += lostAmount;
        }

        return RefundItem.create(orderItem, itemReq.requestQuantity(), itemPointRefundAmount, itemPgRefundAmount);
    }

    /**
     * 재고 복구 따로 처리
     */
    private void restoreStocks(RefundRequest request, Map<Long, OrderItem> orderItemMap) {
        for (RefundItemRequest itemReq : request.items()) {
            orderItemMap.get(itemReq.orderItemId()).getProduct().restoreStock(itemReq.requestQuantity());
        }
    }

    /**
     * 현재 DB 상에서 이미 환불된 PG 금액 총합 반환
     */
    @Transactional(readOnly = true)
    public int getRefundedPgAmount(Long paymentId) {
        return refundRepository.sumRefundedPgAmountByPaymentId(paymentId);
    }

    /**
     * 환불 결과 갱신
     */
    @Transactional
    public void updateRefundResult(Long refundId, boolean isPgSuccess) {
        // [Step 8] 결과 갱신
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 환불 건입니다."));

        if (!isPgSuccess) {
            refund.changeStatus(RefundStatus.FAILED);
            // 실패 시 에러 로그 기록 (재시도 및 수동 보정 대상 표식)
            log.error("[CRITICAL: 수동 보정 요망] PG사 환불 통신 실패! " +
                      "DB 상태(재고, 결제)는 환불 처리되었으나 실제 PG 환불이 누락되었습니다. " +
                      "포트원 관리자 센터에서 수동 취소가 필요합니다. -> Refund ID: {}, Payment ID: {}, PG환불요청액: {}", 
                      refund.getId(), refund.getPayment().getId(), refund.getPgRefundAmount());
        }
        // 통신 성공 시에는 기본적으로 Refund가 생성 시점에 설정된 상태(예: COMPLETED)로 유지됨
    }
}