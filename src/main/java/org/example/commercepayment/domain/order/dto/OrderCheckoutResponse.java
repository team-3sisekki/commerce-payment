package org.example.commercepayment.domain.order.dto;

import org.example.commercepayment.domain.order.entity.Order;
import org.example.commercepayment.domain.order.entity.OrderItem;
import org.example.commercepayment.domain.payment.entity.Payment;

public record OrderCheckoutResponse(
        Long orderId,
        String oderNumber,
        String portonePaymentId,
        int totalPrice,
        int usePoint,
        int pgAmount,
        String orderName,
        String status)
{
    public static OrderCheckoutResponse from(Order order, Payment payment) {
        return new OrderCheckoutResponse(
                order.getId(),
                order.getOrderNumber(),
                payment.getPortonePaymentId(),
                order.getTotalAmount(),
                order.getUsedPoint(),
                payment.getPgAmount(),
                order.getOrderName(),
                order.getStatus().name()
        );
    }
}
