package com.tenantportal.repository;

import com.tenantportal.model.OccupancyHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OccupancyHistoryRepository extends JpaRepository<OccupancyHistory, Long> {
    List<OccupancyHistory> findByUnitId(Long unitId);
    List<OccupancyHistory> findByTenantId(Long tenantId);
}
