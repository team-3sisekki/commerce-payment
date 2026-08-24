package org.example.commercepayment.domain.order.service;

import lombok.RequiredArgsConstructor;
import org.example.commercepayment.domain.member.entity.Member;
import org.example.commercepayment.domain.order.dto.OrderItemResponse;
import org.example.commercepayment.domain.order.dto.OrderResponse;
import org.example.commercepayment.domain.order.entity.Order;
import org.example.commercepayment.domain.order.entity.OrderItem;
import org.example.commercepayment.domain.order.repository.OrderRepository;
import org.example.commercepayment.domain.payment.entity.Payment;
import org.example.commercepayment.global.error.BusinessException;
import org.example.commercepayment.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    // 주문 생성. 재고 선차감은 OrderFacade가 이미 끝낸 상태
    @Transactional
    public Order createOrder(Member member, List<OrderItem> orderItems, int usePoint) {
        Order order = Order.builder()
                .member(member)
                .orderItems(orderItems)
                .usedPoint(usePoint).build();

        return orderRepository.save(order);
    }

    // 내 주문 목록 조회 (최신순)
    public List<Order> findOrderEntities(Long memberId) {
        return orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    // 주문 단건 상세 조회 : orderId만으로 조회
    public Order findOrderEntity(Long orderId) {
        return orderRepository.findByIdWithOrderItems(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    // Order -> OrderResponse 변환, OrderItem -> OrderItemResponse 변환
    public OrderResponse toResponse(Order order, Payment payment) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderResponse.from(order, payment);
    }

    // paymentId로 주문 조회_환불용
    public Order findOrderByPaymentId(Long paymentId) {

        return orderRepository.findByPaymentIdWithOrderItems(paymentId).orElseThrow(
                () -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    // 결제 건의 주문 상품 목록
    public List<OrderItem> findOrderItemsByPaymentId(Long paymentId) {

        return findOrderByPaymentId(paymentId).getOrderItems();
    }

    // 총 주문 수량의 합
    public int getTotalQuantityByPaymentId(Long paymentId) {

        return findOrderByPaymentId(paymentId).getTotalQuantity();
    }

}

