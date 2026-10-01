package com.tenantportal.dto;

import com.tenantportal.model.BillLineItem;

import java.time.LocalDate;
import java.util.List;

public record CreateBillRequest(
        Long unitId,
        Long tenantId,
        Integer billMonth,
        Integer billYear,
        LocalDate dueDate,
        List<LineItemRequest> lineItems
) {
    public record LineItemRequest(
            BillLineItem.Type type,
            String description,
            java.math.BigDecimal amount,
            Double units,
            java.math.BigDecimal ratePerUnit
    ) {}
}
