package org.example.commercepayment.domain.payment.service;

import org.example.commercepayment.domain.cart.service.CartService;
import org.example.commercepayment.domain.order.entity.Order;
import org.example.commercepayment.domain.order.entity.OrderItem;
import org.example.commercepayment.domain.order.entity.OrderStatus;
import org.example.commercepayment.domain.payment.dto.PaymentConfirmResponse;
import org.example.commercepayment.domain.payment.entity.FailReason;
import org.example.commercepayment.domain.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.example.commercepayment.domain.point.service.PointService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentCommandService {

    private static final double POINT_EARN_RATE = 0.01;

    private final PaymentService paymentService;
    private final PointService pointService;
    private final CartService cartService;

    @Transactional
    public void failPaymentAndOrder(Long orderId, FailReason reason) {
        Payment payment = paymentService.findByOrderIdWithOrder(orderId);
        Order order = payment.getOrder();

        paymentService.failPayment(payment, reason);

        // 포인트를 이미 사용 처리했었다면 복구 (전액 카드 결제라 사용액이 0이면 스킵)
        if (payment.getPointUsedAmount() > 0) {
            pointService.restoreUse(order.getMemberId(), payment, payment.getPointUsedAmount());
        }

        order.transitTo(OrderStatus.CANCELED);
        order.getOrderItems().forEach(item ->
                item.getProduct().restoreStock(item.getQuantity()));
    }

    @Transactional
    public PaymentConfirmResponse approvePaymentAndOrder(Long orderId) {
        Payment payment = paymentService.findByOrderIdWithOrder(orderId);
        Order order = payment.getOrder();

        int accruedPoint = (int) (payment.getPgAmount() * POINT_EARN_RATE);

        if (payment.getPointUsedAmount() > 0) {
            pointService.use(order.getMemberId(), payment, payment.getPointUsedAmount());
        }

        paymentService.completePayment(payment, accruedPoint);

        if (accruedPoint > 0) {
            pointService.earn(order.getMemberId(), payment, accruedPoint);
        }

        order.transitTo(OrderStatus.COMPLETED);

        // -----------------------------------------------------------
        // OrderItem 스냅샷의 Product ID 목록 추출 후 장바구니 삭제
        // -----------------------------------------------------------
        List<Long> productIds = order.getOrderItems().stream()
                .map(orderItem -> orderItem.getProduct().getId())
                .toList();

        cartService.deleteCartItemsByProductIds(order.getMemberId(), productIds);

        return PaymentConfirmResponse.from(payment);
    }
}