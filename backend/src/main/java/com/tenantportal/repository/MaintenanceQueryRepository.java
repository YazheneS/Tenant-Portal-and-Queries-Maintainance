package com.tenantportal.repository;

import com.tenantportal.model.MaintenanceQuery;
import com.tenantportal.model.MaintenanceQuery.Status;
import com.tenantportal.model.MaintenanceQuery.Priority;
import com.tenantportal.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MaintenanceQueryRepository extends JpaRepository<MaintenanceQuery, Long> {

    List<MaintenanceQuery> findByTenant(Tenant tenant);

    List<MaintenanceQuery> findByStatus(Status status);

    List<MaintenanceQuery> findByPriority(Priority priority);

    List<MaintenanceQuery> findByTenantAndStatus(Tenant tenant, Status status);

    long countByStatus(Status status);
}
