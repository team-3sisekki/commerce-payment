package org.example.commercepayment.domain.payment.port;

public record PaymentGatewayResponse(
        String id,
        String status,
        int totalAmount,
        int cancelledAmount // 총 취소 금액
) {}