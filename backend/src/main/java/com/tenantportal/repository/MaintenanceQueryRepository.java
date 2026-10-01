package com.tenantportal.repository;

import com.tenantportal.model.MaintenanceQuery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MaintenanceQueryRepository extends JpaRepository<MaintenanceQuery, Long> {
    List<MaintenanceQuery> findByTenantId(Long tenantId);
    List<MaintenanceQuery> findByStatus(MaintenanceQuery.Status status);
    Optional<MaintenanceQuery> findByVendorAccessTokenAndVendorTokenExpiresAtAfter(
            String vendorAccessToken, LocalDateTime now);
}
