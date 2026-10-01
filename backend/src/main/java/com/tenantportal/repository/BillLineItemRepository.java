package com.tenantportal.repository;

import com.tenantportal.model.BillLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillLineItemRepository extends JpaRepository<BillLineItem, Long> {
    List<BillLineItem> findByBillId(Long billId);
}
