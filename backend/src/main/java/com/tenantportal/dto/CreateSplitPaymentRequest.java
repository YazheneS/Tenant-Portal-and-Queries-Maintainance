package com.tenantportal.dto;

import java.math.BigDecimal;
import java.util.List;

public record CreateSplitPaymentRequest(Long billId, List<TenantShare> shares) {
    public record TenantShare(Long tenantId, BigDecimal amount) {}
}
