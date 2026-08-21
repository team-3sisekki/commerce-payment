package org.example.commercepayment.domain.refund.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.commercepayment.domain.refund.dto.RefundRequest;
import org.example.commercepayment.domain.refund.dto.RefundResponse;
import org.example.commercepayment.domain.refund.facade.RefundFacade;
import org.example.commercepayment.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundFacade refundFacade;

    /**
     * 환불 요청 API
     * 인증된 memberId를 함께 전달
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RefundResponse>> processRefund(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody RefundRequest request) {
        RefundResponse response = refundFacade.processRefund(memberId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
