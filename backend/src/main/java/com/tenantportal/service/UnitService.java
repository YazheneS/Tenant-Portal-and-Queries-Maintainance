package com.tenantportal.service;

import com.tenantportal.model.Unit;
import com.tenantportal.repository.UnitRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnitService {

    private final UnitRepository unitRepository;
    private final AuditService auditService;

    public List<Unit> findByPropertyId(Long propertyId) {
        return unitRepository.findByPropertyId(propertyId);
    }

    public Unit getByIdOrThrow(Long id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + id));
    }

    public Unit create(Unit unit, String performedByClerkId) {
        Unit saved = unitRepository.save(unit);
        auditService.log("Unit", saved.getId(), "CREATED", null, saved.getUnitNumber(), performedByClerkId);
        return saved;
    }

    public Unit update(Long id, Unit updates, String performedByClerkId) {
        Unit existing = getByIdOrThrow(id);
        existing.setUnitNumber(updates.getUnitNumber());
        existing.setFloor(updates.getFloor());
        existing.setMonthlyRent(updates.getMonthlyRent());
        Unit saved = unitRepository.save(existing);
        auditService.log("Unit", id, "UPDATED", null, saved.getUnitNumber(), performedByClerkId);
        return saved;
    }
}
