package com.tenantportal.repository;

import com.tenantportal.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByTenantId(Long tenantId);
    List<Payment> findByBillId(Long billId);
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}
