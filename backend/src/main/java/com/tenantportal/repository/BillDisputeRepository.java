package com.tenantportal.repository;

import com.tenantportal.model.BillDispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillDisputeRepository extends JpaRepository<BillDispute, Long> {
    List<BillDispute> findByTenantId(Long tenantId);
    List<BillDispute> findByBillId(Long billId);
    List<BillDispute> findByStatus(BillDispute.Status status);
}
