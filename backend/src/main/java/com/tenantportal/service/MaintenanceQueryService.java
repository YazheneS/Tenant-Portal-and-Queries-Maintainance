package com.tenantportal.service;

import com.tenantportal.model.MaintenanceQuery;
import com.tenantportal.model.MaintenanceQuery.Status;
import com.tenantportal.model.Tenant;
import com.tenantportal.repository.MaintenanceQueryRepository;
import com.tenantportal.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceQueryService {

    private final MaintenanceQueryRepository queryRepository;
    private final TenantRepository tenantRepository;

    public MaintenanceQuery createQuery(Long tenantId, MaintenanceQuery query) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant not found"));
        query.setTenant(tenant);
        return queryRepository.save(query);
    }

    public List<MaintenanceQuery> getQueriesByTenant(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant not found"));
        return queryRepository.findByTenant(tenant);
    }

    public List<MaintenanceQuery> getQueriesByStatus(Status status) {
        return queryRepository.findByStatus(status);
    }

    public MaintenanceQuery updateStatus(Long queryId, Status newStatus) {
        MaintenanceQuery query = queryRepository.findById(queryId)
            .orElseThrow(() -> new RuntimeException("Query not found"));
        query.setStatus(newStatus);
        if (newStatus == Status.RESOLVED) {
            query.setResolvedAt(java.time.LocalDateTime.now());
        }
        return queryRepository.save(query);
    }

    public void deleteQuery(Long queryId) {
        queryRepository.deleteById(queryId);
    }

    public long countByStatus(Status status) {
        return queryRepository.countByStatus(status);
    }
}
