package org.example.commercepayment.domain.payment.service;

import org.example.commercepayment.domain.order.entity.Order;
import org.example.commercepayment.domain.payment.entity.FailReason;
import org.example.commercepayment.domain.payment.entity.Payment;
import org.example.commercepayment.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.example.commercepayment.global.error.BusinessException;
import org.example.commercepayment.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;

    // 결제 생성 — pgAmount는 Payment 생성자가 스스로 계산
    @Transactional
    public Payment createPayment(Order order, int amount, int pointUsedAmount) {
        Payment payment = Payment.builder()
                .order(order)
                .amount(amount)
                .pointUsedAmount(pointUsedAmount)
                .build();
        return paymentRepository.save(payment);
    }

    public Payment findByOrderIdWithOrder(Long orderId) {
        return paymentRepository.findByOrderIdWithOrder(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    public Payment findByOrderId(Long orderId) {
        return findByOrderIdWithOrder(orderId);
    }

    public Payment findByIdWithOrder(Long paymentId) {
        return paymentRepository.findByIdWithOrder(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    public Payment findForRefund(Long paymentId) {
        return paymentRepository.findByIdForRefund(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    public Payment findByPortonePaymentId(String portonePaymentId) {
        return paymentRepository.findByPortonePaymentId(portonePaymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    // 결제 완료 처리
    @Transactional
    public void completePayment(Payment payment, int accruedPoint) {
        payment.complete(accruedPoint);
    }

    // 결제 실패 처리 (상세 사유 지정)
    @Transactional
    public void failPayment(Payment payment, FailReason reason) {
        payment.fail(reason);
    }

    // 결제 상태 변경(Canceled)
    @Transactional
    public void cancelPayment(Payment payment) {
        payment.cancel();
    }

    public Map<Long, Payment> findPaymentMapByOrderIds(List<Long> orderIds) {
        if (orderIds.isEmpty()) return Map.of();
        return paymentRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.toMap(p -> p.getOrder().getId(), p -> p));
    }
}