package com.tenantportal.repository;

import com.tenantportal.model.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByTenantId(Long tenantId);
    List<Bill> findByUnitIdAndBillMonthAndBillYear(Long unitId, Integer billMonth, Integer billYear);
    List<Bill> findByStatus(Bill.Status status);
}
