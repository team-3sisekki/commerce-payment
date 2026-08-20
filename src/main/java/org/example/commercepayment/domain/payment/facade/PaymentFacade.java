package org.example.commercepayment.domain.payment.facade;

import org.example.commercepayment.domain.order.entity.Order;
import org.example.commercepayment.domain.payment.dto.PaymentConfirmRequest;
import org.example.commercepayment.domain.payment.dto.PaymentConfirmResponse;
import org.example.commercepayment.domain.payment.entity.FailReason;
import org.example.commercepayment.domain.payment.entity.Payment;
import org.example.commercepayment.domain.payment.entity.PaymentStatus;
import org.example.commercepayment.domain.payment.port.PaymentGateway;
import org.example.commercepayment.domain.payment.port.PaymentGatewayResponse;
import org.example.commercepayment.domain.payment.service.PaymentCommandService;
import org.example.commercepayment.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commercepayment.global.error.BusinessException;
import org.example.commercepayment.global.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFacade {

    private static final String PG_STATUS_PAID = "PAID";

    private final PaymentService paymentService;
    private final PaymentCommandService paymentCommandService;
    private final PaymentGateway paymentGateway;

    public PaymentConfirmResponse confirmPayment(Long memberId, PaymentConfirmRequest request) {
        Payment payment = paymentService.findByOrderIdWithOrder(request.orderId());
        Order order = payment.getOrder();

        if (!order.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED_PAYMENT);
        }

        String portonePaymentId = payment.getPortonePaymentId();
        if (!portonePaymentId.equals(request.portonePaymentId())) {
            log.warn("결제 승인 거부 — portonePaymentId 불일치: DB={}, 요청={}",
                    portonePaymentId, request.portonePaymentId());
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (payment.getPgAmount() == 0) {
            log.info("전액 포인트 결제 — PG 조회 스킵: paymentId={}", payment.getId());
            return paymentCommandService.approvePaymentAndOrder(order.getId());
        }

        PaymentGatewayResponse pgPayment = paymentGateway.getPayment(portonePaymentId);

        if (!PG_STATUS_PAID.equals(pgPayment.status())) {
            log.error("결제 승인 실패 — PG 상태 비정상: paymentId={}, pgStatus={}",
                    payment.getId(), pgPayment.status());
            paymentCommandService.failPaymentAndOrder(order.getId(), FailReason.PG_DECLINED);
            throw new BusinessException(ErrorCode.PAYMENT_NOT_PAID);
        }

        if (payment.getPgAmount() != pgPayment.totalAmount()) {
            log.error("결제 승인 실패 — 금액 불일치: paymentId={}, DB pgAmount={}, PG금액={}",
                    payment.getId(), payment.getPgAmount(), pgPayment.totalAmount());
            try {
                paymentGateway.cancelPayment(portonePaymentId, "결제 금액 불일치 자동 취소", null);
            } catch (Exception e) {
                log.error("PG 자동 취소 실패 : 수동 처리 필요: portonePaymentId={}", portonePaymentId, e);
            }
            paymentCommandService.failPaymentAndOrder(order.getId(), FailReason.AMOUNT_MISMATCH);
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        return paymentCommandService.approvePaymentAndOrder(order.getId());
    }
}