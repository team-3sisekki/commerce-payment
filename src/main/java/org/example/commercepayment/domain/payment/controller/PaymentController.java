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

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentConfirmResponse>> confirm(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(paymentFacade.confirmPayment(memberId, request)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<PaymentCancelResponse>> cancel(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) PaymentCancelRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(paymentFacade.cancelPayment(memberId, id, request)));
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<ApiResponse<PaymentCancelResponse>> fail(
            @PathVariable Long id,
            @RequestBody FailRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(paymentFacade.failPayment(id, request.failReason())));
    }

    public record FailRequest(FailReason failReason) {}
}