package com.tenantportal.repository;

import com.tenantportal.model.PaymentSplit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentSplitRepository extends JpaRepository<PaymentSplit, Long> {
    List<PaymentSplit> findByPaymentId(Long paymentId);
    List<PaymentSplit> findByTenantId(Long tenantId);
}
