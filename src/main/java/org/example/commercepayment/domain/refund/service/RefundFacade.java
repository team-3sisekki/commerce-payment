package org.example.commercepayment.domain.refund.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commercepayment.domain.payment.entity.Payment;
import org.example.commercepayment.domain.payment.port.PaymentGateway;
import org.example.commercepayment.domain.payment.port.PaymentGatewayResponse;
import org.example.commercepayment.domain.payment.service.PaymentService;
import org.example.commercepayment.domain.refund.dto.RefundRequest;
import org.example.commercepayment.domain.refund.dto.RefundResponse;
import org.example.commercepayment.domain.refund.entity.Refund;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundFacade {

    private final RefundService refundService;
    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;

    public RefundResponse processRefund(Long memberId, RefundRequest request) {
        log.info("========== [환불 파사드 진입] ==========");
        log.info("결제ID={}, 회원ID={}, 취소사유={}", request.paymentId(), memberId, request.cancelReason());

        /*PG 선검증*/
        // 1. 결제 단건 조회
        Payment payment = paymentService.findByIdWithOrder(request.paymentId());
        
        // 2. PG사 결제 내역 조회 및 상태 검증 (단, PG 결제액이 0원인 전액 포인트 결제는 스킵)
        if (payment.getPgAmount() > 0) {
            PaymentGatewayResponse pgResponse = paymentGateway.getPayment(payment.getPortonePaymentId());
            
            // 3. 환불액 정합성 철벽 검증 (DB 잔액 vs PG 잔액)
            int pgRemainingAmount = pgResponse.totalAmount() - pgResponse.cancelledAmount();
            log.info("PG 환불 잔액 = {}", pgRemainingAmount);
            int dbRemainingAmount = payment.getPgAmount() - refundService.getRefundedPgAmount(payment.getId());
            log.info("DB 환불 잔액 = {}", dbRemainingAmount);
            
            if (pgRemainingAmount != dbRemainingAmount) {
                throw new IllegalStateException("DB와 PG사의 결제 잔액이 일치하지 않습니다. 관리자 확인이 필요합니다.");
            }
        }

        /*선검증 및 DB 갱신*/
        Refund savedRefund = refundService.calculateAndSaveRefund(memberId, request);

        /*PG 결제 취소 호출*/
        boolean isPgSuccess = false;
        if (savedRefund.getPgRefundAmount() == 0) {
            log.info("PG 환불 금액이 0원이므로 PG사 통신을 스킵합니다. Refund ID: {}", savedRefund.getId());
            isPgSuccess = true;
        } else {
            try {
                paymentGateway.cancelPayment(
                    payment.getPortonePaymentId(), 
                    request.cancelReason(), 
                    savedRefund.getPgRefundAmount()
                );
                isPgSuccess = true;
                log.info("PG사 환불 통신 성공. Refund ID: {}", savedRefund.getId());
            } catch (Exception e) {
                log.error("PG사 환불 통신 실패. Refund ID: {}, Reason: {}", savedRefund.getId(), e.getMessage());
            }
        }

        // 6. 결과 갱신
        refundService.updateRefundResult(savedRefund.getId(), isPgSuccess);
        return RefundResponse.from(savedRefund);
    }
}