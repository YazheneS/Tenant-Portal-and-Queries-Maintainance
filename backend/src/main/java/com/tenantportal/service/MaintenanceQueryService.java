package com.tenantportal.service;

import com.tenantportal.model.MaintenanceQuery;
import com.tenantportal.model.Tenant;
import com.tenantportal.model.Vendor;
import com.tenantportal.repository.MaintenanceQueryRepository;
import com.tenantportal.repository.VendorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Module 3 — Core Complaint Flow.
 *
 * Also owns vendor access token issuance: when an Owner assigns a vendor,
 * this generates the UUID + expiry pair on the MaintenanceQuery itself
 * (fields already exist on the entity), which VendorController then
 * validates on every vendor-facing request instead of a JWT.
 */
@Service
@RequiredArgsConstructor
public class MaintenanceQueryService {

    /** How long a vendor's link stays valid after assignment. */
    private static final long VENDOR_TOKEN_VALIDITY_HOURS = 72;

    private final MaintenanceQueryRepository maintenanceQueryRepository;
    private final VendorRepository vendorRepository;
    private final AuditService auditService;
    private final EmailService emailService;

    public List<MaintenanceQuery> findByTenantId(Long tenantId) {
        return maintenanceQueryRepository.findByTenantId(tenantId);
    }

    public List<MaintenanceQuery> findByStatus(MaintenanceQuery.Status status) {
        return maintenanceQueryRepository.findByStatus(status);
    }

    public MaintenanceQuery getByIdOrThrow(Long id) {
        return maintenanceQueryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MaintenanceQuery not found: " + id));
    }

    /** Tenant raises a new complaint. */
    @Transactional
    public MaintenanceQuery raise(Tenant tenant, MaintenanceQuery query) {
        if (tenant.getUnit() == null) {
            throw new IllegalStateException(
                    "Tenant " + tenant.getId() + " is not assigned to a unit yet");
        }
        query.setTenant(tenant);
        query.setUnit(tenant.getUnit());
        query.setStatus(MaintenanceQuery.Status.OPEN);
        MaintenanceQuery saved = maintenanceQueryRepository.save(query);
        auditService.log("MaintenanceQuery", saved.getId(), "RAISED",
                null, saved.getTitle(), tenant.getClerkUserId());
        emailService.notifyOwnerNewComplaint(saved);
        return saved;
    }

    /**
     * Owner assigns a vendor. Generates the tokenized access link at this
     * point — not before — since the token is scoped to this specific job
     * assignment.
     */
    @Transactional
    public MaintenanceQuery assignVendor(Long queryId, Long vendorId, String performedByClerkId) {
        MaintenanceQuery query = getByIdOrThrow(queryId);
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new EntityNotFoundException("Vendor not found: " + vendorId));

        query.setAssignedVendor(vendor);
        query.setStatus(MaintenanceQuery.Status.ASSIGNED);
        query.setVendorAccessToken(UUID.randomUUID().toString());
        query.setVendorTokenExpiresAt(LocalDateTime.now().plusHours(VENDOR_TOKEN_VALIDITY_HOURS));

        MaintenanceQuery saved = maintenanceQueryRepository.save(query);
        auditService.log("MaintenanceQuery", queryId, "VENDOR_ASSIGNED",
                null, "vendor=" + vendorId, performedByClerkId);
        emailService.notifyMaintenanceStatusChanged(saved);
        return saved;
    }

    /** Looks up a query by its vendor token, only if not expired. Used by VendorController. */
    public MaintenanceQuery getByValidVendorToken(String token) {
        return maintenanceQueryRepository
                .findByVendorAccessTokenAndVendorTokenExpiresAtAfter(token, LocalDateTime.now())
                .orElseThrow(() -> new EntityNotFoundException("Invalid or expired vendor token"));
    }

    /** Vendor updates status via their tokenized link — no Clerk auth involved. */
    @Transactional
    public MaintenanceQuery updateStatusByVendorToken(String token, MaintenanceQuery.Status newStatus,
                                                        String resolutionNotes) {
        MaintenanceQuery query = getByValidVendorToken(token);
        MaintenanceQuery.Status oldStatus = query.getStatus();
        query.setStatus(newStatus);

        if (newStatus == MaintenanceQuery.Status.RESOLVED) {
            query.setResolvedAt(LocalDateTime.now());
            query.setResolutionNotes(resolutionNotes);
            if (query.getAssignedVendor() != null) {
                Vendor vendor = query.getAssignedVendor();
                vendor.setTotalJobsCompleted(vendor.getTotalJobsCompleted() + 1);
                vendorRepository.save(vendor);
            }
        }

        MaintenanceQuery saved = maintenanceQueryRepository.save(query);
        auditService.log("MaintenanceQuery", query.getId(), "STATUS_CHANGED_BY_VENDOR",
                oldStatus.name(), newStatus.name(), "vendor-token");
        emailService.notifyMaintenanceStatusChanged(saved);
        return saved;
    }

    @Transactional
    public MaintenanceQuery close(Long queryId, String performedByClerkId) {
        MaintenanceQuery query = getByIdOrThrow(queryId);
        query.setStatus(MaintenanceQuery.Status.CLOSED);
        MaintenanceQuery saved = maintenanceQueryRepository.save(query);
        auditService.log("MaintenanceQuery", queryId, "CLOSED",
                null, null, performedByClerkId);
        return saved;
    }
}
