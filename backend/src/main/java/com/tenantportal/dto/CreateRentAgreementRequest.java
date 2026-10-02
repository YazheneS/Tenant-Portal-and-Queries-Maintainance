package com.tenantportal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateRentAgreementRequest(
        Long unitId,
        Long tenantId,
        BigDecimal baseRent,
        BigDecimal escalationPercent,
        Integer escalationDayOfYear,
        LocalDate startDate
) {
}
