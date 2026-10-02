package com.tenantportal.dto;

import java.math.BigDecimal;

public record CreateUnitRequest(
        String unitNumber,
        Integer floor,
        Long propertyId,
        BigDecimal monthlyRent
) {
}
