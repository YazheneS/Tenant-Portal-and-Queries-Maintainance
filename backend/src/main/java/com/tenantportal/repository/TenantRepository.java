package com.tenantportal.repository;

import com.tenantportal.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByClerkUserId(String clerkUserId);
    Optional<Tenant> findByEmail(String email);
}
