package com.tenantportal.dto;

import com.tenantportal.model.BillLineItem;

import java.math.BigDecimal;

public record AddLineItemRequest(
        BillLineItem.Type type,
        String description,
        BigDecimal amount,
        Double units,
        BigDecimal ratePerUnit
) {
}
