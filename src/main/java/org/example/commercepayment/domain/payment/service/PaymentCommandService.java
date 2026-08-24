package org.example.commercepayment.domain.payment.service;

import org.example.commercepayment.domain.cart.service.CartService;
import org.example.commercepayment.domain.order.entity.Order;
import org.example.commercepayment.domain.order.entity.OrderItem;
import org.example.commercepayment.domain.order.entity.OrderStatus;
import org.example.commercepayment.domain.payment.dto.PaymentConfirmResponse;
import org.example.commercepayment.domain.payment.entity.FailReason;
import org.example.commercepayment.domain.payment.entity.Payment;
import org.example.commercepayment.domain.payment.entity.PaymentStatus;
import org.example.commercepayment.domain.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentCommandService {

    private static final double POINT_EARN_RATE = 0.01;

    private final PaymentService paymentService;
    private final PointService pointService;
    private final CartService cartService;

    /**
     * 결제 승인 + 주문 완료
     */
    @Transactional
    public PaymentConfirmResponse approvePaymentAndOrder(Long orderId) {
        Payment payment =
                paymentService.findByOrderIdWithOrderForUpdate(orderId);

        // Confirm API와 Webhook이 동시에 처리된 경우
        // 먼저 완료한 요청이 있다면 나머지는 정상 종료
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return PaymentConfirmResponse.from(payment);
        }

        Order order = payment.getOrder();

        // PG 결제 금액을 기준으로 적립 포인트 계산
        int accruedPoint = (int) (payment.getPgAmount() * POINT_EARN_RATE);

        // 사용 포인트 차감
        if (payment.getPointUsedAmount() > 0) {
            pointService.use(order.getMemberId(), payment, payment.getPointUsedAmount());
        }

        // Payment 완료
        paymentService.completePayment(payment, accruedPoint);

        // 적립 포인트 지급
        if (accruedPoint > 0) {
            pointService.earn(order.getMemberId(), payment, accruedPoint);
        }

        // Order 완료
        order.transitTo(OrderStatus.COMPLETED);

        // 장바구니 상품 삭제
        deleteCartItems(order);

        return PaymentConfirmResponse.from(payment);
    }

    /**
     * 사용자 취소 또는 PG 실패로 인한
     * 결제 실패 + 주문 취소
     */
    @Transactional
    public void failPaymentAndOrder(Long orderId, FailReason reason) {
        Payment payment = paymentService.findByOrderIdWithOrder(orderId);

        Order order = payment.getOrder();

        // Payment 실패 처리
        paymentService.failPayment(payment, reason);

        // 사용 포인트 복구
        restoreUsedPoint(payment, order);

        // Order 취소
        order.transitTo(OrderStatus.CANCELED);

        // 재고 복구
        restoreStock(order);
    }

    /**
     * 사용자 결제 취소
     */
    @Transactional
    public void cancelPaymentAndOrder(Long orderId, FailReason reason) {
        Payment payment = paymentService.findByOrderIdWithOrder(orderId);

        Order order = payment.getOrder();

        // Payment 취소 처리
        paymentService.cancelPayment(payment);

        // 사용 포인트 복구
        restoreUsedPoint(payment, order);

        // Order 취소
        order.transitTo(OrderStatus.CANCELED);

        // 재고 복구
        restoreStock(order);
    }

    /**
     * 사용 포인트 복구
     */
    private void restoreUsedPoint(Payment payment, Order order) {
        if (payment.getPointUsedAmount() > 0) {
            pointService.restoreUse(order.getMemberId(), payment, payment.getPointUsedAmount());
        }
    }

    /**
     * 주문 상품 재고 복구
     */
    private void restoreStock(Order order) {
        order.getOrderItems().forEach(item -> item.getProduct().restoreStock(item.getQuantity()));
    }

    /**
     * 결제 완료 후 장바구니 상품 삭제
     */
    private void deleteCartItems(Order order) {

        List<Long> productIds = order.getOrderItems()
                .stream()
                .map(OrderItem::getProduct)
                .map(product -> product.getId())
                .toList();

        cartService.deleteCartItemsByProductIds(order.getMemberId(), productIds);
    }
}