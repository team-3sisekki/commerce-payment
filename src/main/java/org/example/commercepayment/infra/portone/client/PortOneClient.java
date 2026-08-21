package org.example.commercepayment.infra.portone.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commercepayment.domain.payment.port.PaymentGateway;
import org.example.commercepayment.domain.payment.port.PaymentGatewayResponse;
import org.example.commercepayment.infra.portone.config.PortOneProperties;
import org.example.commercepayment.infra.portone.dto.PortOneCancelRequest;
import org.example.commercepayment.infra.portone.dto.PortOnePaymentResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortOneClient implements PaymentGateway {

    private final RestClient portOneRestClient;
    private final PortOneProperties portOneProperties;

    @Override
    public PaymentGatewayResponse getPayment(String paymentId) {
        // paymentId logging
        log.info("PortOne 결제 조회: {}", paymentId);

        // portOneRestClient https://api.portone.io/payments/{paymnetId}?storeId={storeId}
        PortOnePaymentResponse response = portOneRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/payments/{paymentId}")
                        .queryParam("storeId", portOneProperties.getStoreId())
                        .build(paymentId))
                .retrieve()
                .body(PortOnePaymentResponse.class);

        // PortOne 응답 결과인 PortOnePaymentResponse를 PaymentGatewayResponse로 변환
        return new PaymentGatewayResponse(
                response.id(),
                response.status(),
                response.amount().total(),
                response.amount().cancelled()
        );
    }

    @Override
    public void cancelPayment(String paymentId, String reason, Integer amount) {
        // paymentId logging
        log.info("PortOne 결제 취소 요청: paymentId={}, reason={}, amount={}", paymentId, reason, amount);

        // portOneRestClient https://api.portone.io/payments/{paymnetId}/cancel body: {reason, storeId}
        portOneRestClient.post()
                .uri("/payments/{paymentId}/cancel", paymentId)
                .body(new PortOneCancelRequest(amount, reason, portOneProperties.getStoreId()))
                .retrieve()
                .toBodilessEntity();
    }
}