package com.tenantportal.service;

import com.tenantportal.model.AuditLog;
import com.tenantportal.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Central place to write AuditLog rows. Every service that mutates a
 * tenant-visible entity (Bill, MaintenanceQuery, BillDispute, etc.) should
 * call this instead of writing AuditLog rows inline, so the "every state
 * change logged" requirement in the master plan stays consistent in one place.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(String entityType, Long entityId, String action,
                     String oldValue, String newValue, String performedByClerkId) {
        AuditLog entry = new AuditLog();
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setOldValue(oldValue);
        entry.setNewValue(newValue);
        entry.setPerformedBy(performedByClerkId);
        auditLogRepository.save(entry);
    }
}
