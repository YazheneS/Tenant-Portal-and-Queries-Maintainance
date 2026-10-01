package com.tenantportal.service;

import com.tenantportal.model.OccupancyHistory;
import com.tenantportal.model.Tenant;
import com.tenantportal.model.Unit;
import com.tenantportal.repository.OccupancyHistoryRepository;
import com.tenantportal.repository.TenantRepository;
import com.tenantportal.repository.UnitRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UnitRepository unitRepository;
    private final OccupancyHistoryRepository occupancyHistoryRepository;
    private final AuditService auditService;

    public Optional<Tenant> findByClerkUserId(String clerkUserId) {
        return tenantRepository.findByClerkUserId(clerkUserId);
    }

    public Tenant getByClerkUserIdOrThrow(String clerkUserId) {
        return tenantRepository.findByClerkUserId(clerkUserId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No tenant record for clerkUserId=" + clerkUserId));
    }

    public List<Tenant> findAll() {
        return tenantRepository.findAll();
    }

    /**
     * Called from the Clerk webhook when a user.created event fires, or by an
     * Owner explicitly inviting a tenant. If a Tenant with this clerkUserId
     * already exists, returns it unchanged rather than duplicating.
     */
    @Transactional
    public Tenant syncFromClerk(String clerkUserId, String name, String email, String phone) {
        return tenantRepository.findByClerkUserId(clerkUserId)
                .orElseGet(() -> {
                    Tenant tenant = new Tenant();
                    tenant.setClerkUserId(clerkUserId);
                    tenant.setName(name);
                    tenant.setEmail(email);
                    tenant.setPhone(phone);
                    tenant.setIsActive(true);
                    Tenant saved = tenantRepository.save(tenant);
                    auditService.log("Tenant", saved.getId(), "CREATED_FROM_CLERK",
                            null, email, clerkUserId);
                    return saved;
                });
    }

    @Transactional
    public Tenant assignToUnit(Long tenantId, Long unitId, String performedByClerkId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + unitId));

        tenant.setUnit(unit);
        tenant.setMoveInDate(LocalDate.now());
        unit.setIsOccupied(true);
        unitRepository.save(unit);
        Tenant saved = tenantRepository.save(tenant);

        OccupancyHistory history = new OccupancyHistory();
        history.setUnit(unit);
        history.setTenant(saved);
        history.setMoveInDate(LocalDate.now());
        occupancyHistoryRepository.save(history);

        auditService.log("Tenant", tenant.getId(), "ASSIGNED_TO_UNIT",
                null, "unit=" + unitId, performedByClerkId);
        return saved;
    }

    /**
     * Ends a tenant's occupancy: closes their open OccupancyHistory row with
     * today's date, frees up the Unit, and clears the tenant's unit link.
     * outstandingAmount is whatever's still owed on exit (from unpaid bills),
     * passed in by the caller rather than computed here — billing is
     * BillService's concern.
     */
    @Transactional
    public Tenant moveOut(Long tenantId, BigDecimal outstandingAmount, String notes, String performedByClerkId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));
        Unit unit = tenant.getUnit();
        if (unit == null) {
            throw new IllegalStateException("Tenant " + tenantId + " has no current unit to move out of");
        }

        List<OccupancyHistory> openRecords = occupancyHistoryRepository.findByTenantId(tenantId);
        openRecords.stream()
                .filter(h -> h.getMoveOutDate() == null && h.getUnit().getId().equals(unit.getId()))
                .findFirst()
                .ifPresent(history -> {
                    history.setMoveOutDate(LocalDate.now());
                    history.setOutstandingAmountAtExit(outstandingAmount);
                    history.setNotes(notes);
                    occupancyHistoryRepository.save(history);
                });

        tenant.setUnit(null);
        tenant.setMoveOutDate(LocalDate.now());
        tenant.setIsActive(false);
        unit.setIsOccupied(false);
        unitRepository.save(unit);
        Tenant saved = tenantRepository.save(tenant);

        auditService.log("Tenant", tenantId, "MOVED_OUT",
                null, "outstanding=" + outstandingAmount, performedByClerkId);
        return saved;
    }
}
