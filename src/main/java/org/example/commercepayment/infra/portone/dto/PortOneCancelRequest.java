package org.example.commercepayment.infra.portone.dto;

public record PortOneCancelRequest(
        String reason,   // [필수] 취소 사유
        String storeId   // [조건부] 하위 상점 사용 시 필수
) {}