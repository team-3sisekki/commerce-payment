package org.example.commercepayment.domain.payment.controller;

import org.example.commercepayment.domain.payment.dto.PaymentCancelRequest;
import org.example.commercepayment.domain.payment.dto.PaymentCancelResponse;
import org.example.commercepayment.domain.payment.dto.PaymentConfirmRequest;
import org.example.commercepayment.domain.payment.dto.PaymentConfirmResponse;
import org.example.commercepayment.domain.payment.entity.FailReason;
import org.example.commercepayment.domain.payment.entity.Payment;
import org.example.commercepayment.domain.payment.facade.PaymentFacade;
import org.example.commercepayment.domain.payment.service.PaymentCommandService;
import org.example.commercepayment.domain.payment.service.PaymentService;
import org.example.commercepayment.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentFacade paymentFacade;
    private final PaymentService paymentService;
    private final PaymentCommandService paymentCommandService;

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentConfirmResponse>> confirm(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(paymentFacade.confirmPayment(memberId, request)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<PaymentCancelResponse>> cancel(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) PaymentCancelRequest request
    ) {
        Payment payment = paymentService.findByIdWithOrder(id);
        paymentCommandService.failPaymentAndOrder(payment.getOrder().getId(), FailReason.USER_CANCELLED);

        Payment updated = paymentService.findByIdWithOrder(id);

        String message = (request != null && request.reason() != null && !request.reason().isBlank())
                ? request.reason()
                : "결제가 취소되었습니다.";

        return ResponseEntity.ok(ApiResponse.ok(PaymentCancelResponse.from(updated, message)));
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<ApiResponse<PaymentCancelResponse>> fail(
            @PathVariable Long id,
            @RequestBody FailRequest request
    ) {
        Payment payment = paymentService.findByIdWithOrder(id);
        paymentService.failPayment(payment, request.failReason());
        return ResponseEntity.ok(ApiResponse.ok(PaymentCancelResponse.from(payment, "결제가 실패 처리되었습니다.")));
    }

    public record FailRequest(FailReason failReason) {}
}