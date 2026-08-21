package org.example.commercepayment.domain.order.dto;

import org.example.commercepayment.domain.order.entity.Order;
import org.example.commercepayment.domain.payment.entity.Payment;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        String orderNumber,
        Long paymentId,
        int totalPrice,
        int usedPoint,                      // 사용 포인트
        int pgAmount,                       // PG 결제 금액
        int accruedPoint,                   //  적립 포인트
        String status,
        String paymentStatus,               // 결제 상태
        String orderName,
        LocalDateTime createdAt,
        List<OrderItemResponse> orderItems)
{

    public static OrderResponse from(Order order, Payment payment) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                payment.getId(),
                order.getTotalAmount(),
                order.getUsedPoint(),
                payment.getPgAmount(),
                payment.getEarnedPointAmount(),
                order.getStatus().name(),
                payment.getStatus().name(),
                order.getOrderName(),
                order.getCreatedAt(),
                order.getOrderItems().stream()
                        .map(OrderItemResponse::from)   // 중첩 DTO도 from으로
                        .toList()
        );
    }
}

