package org.example.commercepayment.domain.refund.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commercepayment.domain.order.entity.OrderStatus;
import org.example.commercepayment.domain.refund.dto.RefundHistoryResponse;
import org.example.commercepayment.domain.refund.dto.RefundRequest;
import org.example.commercepayment.domain.refund.dto.RefundRequest.RefundItemRequest;
import org.example.commercepayment.domain.refund.dto.RefundedQuantityDto;
import org.example.commercepayment.domain.refund.entity.Refund;
import org.example.commercepayment.domain.refund.entity.RefundItem;
import org.example.commercepayment.domain.refund.entity.RefundStatus;
import org.example.commercepayment.domain.refund.repository.RefundItemRepository;
import org.example.commercepayment.domain.refund.repository.RefundRepository;
import org.example.commercepayment.domain.order.entity.OrderItem;
import org.example.commercepayment.domain.payment.entity.Payment;
import org.example.commercepayment.domain.payment.service.PaymentService;
import org.example.commercepayment.domain.point.service.PointService;
import org.example.commercepayment.global.error.BusinessException;
import org.example.commercepayment.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

import org.example.commercepayment.domain.product.service.ProductService;
import org.example.commercepayment.domain.refund.dto.RefundableItemResponse;
import org.example.commercepayment.domain.refund.component.RefundCalculator;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final PointService pointService;
    private final PaymentService paymentService;
    private final ProductService productService;
    private final RefundCalculator refundCalculator;
    private final RefundRepository refundRepository;
    private final RefundItemRepository refundItemRepository;



    /**
     * 환불 가능한 상품 목록 및 남은 수량 조회 (사용자 화면 출력용)
     */
    @Transactional(readOnly = true)
    public List<RefundableItemResponse> getRefundableItems(Long memberId, Long paymentId) {
        Payment payment = paymentService.findByIdWithOrderAndItems(paymentId);
        validatePaymentOwnership(payment, memberId);

        Map<Long, Integer> remainQuantities = calculateRemainQuantities(payment.getOrder().getOrderItems());

        return payment.getOrder().getOrderItems().stream()
                .map(item -> RefundableItemResponse.from(
                        item,
                        remainQuantities.getOrDefault(item.getId(), 0)
                ))
                .toList();
    }

    /**
     * 주문 상품들의 '잔여 환불 가능 수량'을 일괄 계산
     */
    private Map<Long, Integer> calculateRemainQuantities(List<OrderItem> orderItems) {
        List<Long> itemIds = orderItems.stream().map(OrderItem::getId).toList();
        if (itemIds.isEmpty()) return Map.of();

        Map<Long, Long> refundedMap = refundItemRepository.findRefundedQuantitiesByOrderItemIds(itemIds).stream()
                .collect(toMap(
                        RefundedQuantityDto::orderItemId,
                        RefundedQuantityDto::refundedQuantity
                ));

        return orderItems.stream().collect(toMap(
                OrderItem::getId,
                item -> item.getQuantity() - refundedMap.getOrDefault(item.getId(), 0L).intValue()
        ));
    }

    /**
     * 환불 내역 조회 (영수증용)
     */
    @Transactional(readOnly = true)
    public List<RefundHistoryResponse> getRefundHistory(Long memberId, Long paymentId) {
        Payment payment = paymentService.findByIdWithOrder(paymentId);
        validatePaymentOwnership(payment, memberId);

        List<Refund> refunds = refundRepository.findByPaymentIdWithItems(paymentId);
        return refunds.stream()
                .map(RefundHistoryResponse::from)
                .toList();
    }

    /**
     * 선검증 및 DB 갱신
     */
    @Transactional
    public Refund calculateAndSaveRefund(Long memberId, RefundRequest request) {
        log.info("========== [환불 처리 시작] ==========");
        log.info("요청 정보: 결제ID={}, 회원ID={}, 환불상품갯수={}", request.paymentId(), memberId, request.items().size());

        Payment payment = paymentService.findForRefund(request.paymentId());
        List<Refund> existingRefunds = refundRepository.findByPaymentIdWithItems(request.paymentId());

        /* 5초 이내에 동일한 결제건으로 환불된 내역이 있는지 확인 (메모리에서 처리)*/
        boolean isDuplicated = existingRefunds.stream()
                .anyMatch(refund -> refund.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(5)));
        if (isDuplicated) {
            throw new BusinessException(ErrorCode.DUPLICATE_REFUND_REQUEST);
        }

        validatePaymentOwnership(payment, memberId);
        payment.validateRefundable();

        List<Refund> completedRefunds = existingRefunds.stream()
                .filter(refund -> refund.getStatus() == RefundStatus.COMPLETED || refund.getStatus() == RefundStatus.PG_FAILED)
                .toList();

        /* 환불 대상 상품의 잔여 환불 가능 수량 초과 여부 확인*/
        Map<Long, OrderItem> orderItemMap = payment.getOrder().getOrderItems().stream()
                .collect(toMap(OrderItem::getId, orderItem -> orderItem));

        Map<Long, Integer> refundedQuantityMap = completedRefunds.stream()
                .flatMap(refund -> refund.getRefundItems().stream())
                .collect(Collectors.groupingBy(
                        refundItem -> refundItem.getOrderItem().getId(),
                        Collectors.summingInt(RefundItem::getRefundQuantity)
                ));


        for (RefundItemRequest item : request.items()) {
            if (!orderItemMap.containsKey(item.orderItemId())) {
                throw new BusinessException(ErrorCode.REFUND_ITEM_NOT_FOUND);
            }

            int originalQty = orderItemMap.get(item.orderItemId()).getQuantity();
            int refundedQty = refundedQuantityMap.getOrDefault(item.orderItemId(), 0);
            int remainingQuantity = originalQty - refundedQty;

            // 이번에 환불해달라고 요청한 수량이, 남은 수량보다 많으면 에러
            if (item.requestQuantity() > remainingQuantity) {
                throw new BusinessException(ErrorCode.EXCEED_REFUNDABLE_QUANTITY);
            }
        }

        /* '총 상품 개수' - '현재 남은 환불 가능 개수' 로 전액/마지막 환불인지 부분환불 인지 분개처리*/
        int totalRequestedQuantity = request.items().stream()
                .mapToInt(RefundItemRequest::requestQuantity)
                .sum();

        int totalOriginalPaymentQuantity = payment.getOrder().getTotalQuantity();
        int totalRefundedPaymentQuantity = completedRefunds.stream()
                .flatMap(r -> r.getRefundItems().stream())
                .mapToInt(RefundItem::getRefundQuantity)
                .sum();
        int paymentRemainingQuantity = totalOriginalPaymentQuantity - totalRefundedPaymentQuantity;

        boolean isFullRefund = (totalRequestedQuantity == paymentRemainingQuantity);

        log.info("전액환불여부={} (요청수량={}, 남은결제수량={})", isFullRefund, totalRequestedQuantity, paymentRemainingQuantity);

        /* 기 환불된 금액 메모리에서 계산 (RefundCalculator에 넘겨줄 용도)*/
        int refundedPgAmount = completedRefunds.stream().mapToInt(Refund::getPgRefundAmount).sum();
        int refundedPointAmount = completedRefunds.stream().mapToInt(Refund::getPointRefundAmount).sum();
        int refundedPointRecoveryAmount = completedRefunds.stream().mapToInt(Refund::getPointRecoveryAmount).sum();

        RefundCalculator.RefundCalculationResult calcResult = refundCalculator.calculate(
                request, orderItemMap, payment, isFullRefund, refundedPgAmount, refundedPointAmount, refundedPointRecoveryAmount
        );

        if (isFullRefund) {
            // 전액 환불이므로 결제와 주문 상태를 모두 '환불/취소' 상태로 변경
            payment.fullRefund();
            payment.getOrder().transitTo(OrderStatus.CANCELED);
        } else {
            // 부분 환불이므로 결제 상태만 바꾸고, 주문 상태는 그대로 유지한다.
            payment.partialRefund();
        }

        List<Long> productIdsToRestore = request.items().stream()
                .map(itemReq -> orderItemMap.get(itemReq.orderItemId()).getProduct().getId())
                .distinct()
                .sorted()
                .toList();

        if (!productIdsToRestore.isEmpty()) {
            productService.findAllByIdForUpdate(productIdsToRestore);
        }

        /*환불된 상품 재고 수량 원복 (+)*/
        restoreStocks(request, orderItemMap);

       /* 사용 포인트를 복구해주고, 적립해준 포인트를 회수한다.*/
        if (calcResult.totalPointRefundAmount() > 0) {
            pointService.restoreUse(payment.getOrder().getMemberId(), payment, calcResult.totalPointRefundAmount());
        }
        if (calcResult.pointRecoveryAmount() > 0) {
            pointService.revokeEarn(payment.getOrder().getMemberId(), payment, calcResult.pointRecoveryAmount());
        }

        Refund refund = Refund.builder()
                .payment(payment)
                .cancelReason(request.cancelReason())
                .pointRefundAmount(calcResult.totalPointRefundAmount())
                .pgRefundAmount(calcResult.totalPgRefundAmount())
                .pointRecoveryAmount(calcResult.pointRecoveryAmount())
                .build();

        Refund savedRefund = refundRepository.save(refund);

        for (RefundItem refundItem : calcResult.refundItems()) {
            savedRefund.addRefundItem(refundItem);
        }

        return savedRefund;
    }

    /**
     * 본인 소유 결제 건인지 검증
     */
    private void validatePaymentOwnership(Payment payment, Long memberId) {
        if (!payment.getOrder().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.REFUND_ACCESS_DENIED);
        }
    }

    /**
     * 재고 복구 처리
     */
    private void restoreStocks(RefundRequest request, Map<Long, OrderItem> orderItemMap) {
        for (RefundItemRequest requestItem : request.items()) {
            orderItemMap.get(requestItem.orderItemId()).getProduct().restoreStock(requestItem.requestQuantity());
        }
    }

    /**
     * 현재 DB 상에서 이미 환불된 PG 금액 총합 반환
     */
    public int getRefundedPgAmount(Long paymentId) {
        return refundRepository.sumRefundedPgAmountByPaymentId(paymentId);
    }

    /**
     * 환불 결과 갱신
     */
    @Transactional
    public void updateRefundResult(Long refundId, boolean isPgSuccess) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));

        if (!isPgSuccess) {
            refund.changeStatus(RefundStatus.PG_FAILED);
            // 실패 시 에러 로그 기록 (재시도 및 수동 보정 대상 표식)
            log.error("[CRITICAL: 수동 보정 요망] PG사 환불 통신 실패! " +
                            "DB 상태(재고, 결제)는 환불 처리되었으나 실제 PG 환불이 누락되었습니다. " +
                            "포트원 관리자 센터에서 수동 취소가 필요합니다. -> Refund ID: {}, Payment ID: {}, PG환불요청액: {}",
                    refund.getId(), refund.getPayment().getId(), refund.getPgRefundAmount());
        }
    }
    
    @Transactional
    public Refund calculateAndSaveFullRefundForSync(Long paymentId, String reason) {
        Payment payment = paymentService.findForRefund(paymentId);
        Long memberId = payment.getOrder().getMemberId();

        RefundRequest fullRequest = buildFullRefundRequest(payment, reason);
        return calculateAndSaveRefund(memberId, fullRequest);
    }

    private RefundRequest buildFullRefundRequest(Payment payment, String reason) {
        Map<Long, Integer> remainQuantities = calculateRemainQuantities(payment.getOrder().getOrderItems());

        List<RefundItemRequest> items = payment.getOrder().getOrderItems().stream()
                .map(orderItem -> {
                    int remaining = remainQuantities.getOrDefault(orderItem.getId(), 0);
                    return remaining > 0 ? new RefundItemRequest(orderItem.getId(), remaining) : null;
                })
                .filter(Objects::nonNull)
                .toList();

        return new RefundRequest(payment.getId(), reason, items);
    }
}