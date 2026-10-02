package com.tenantportal.service;

import com.tenantportal.dto.CreateUnitRequest;
import com.tenantportal.model.Property;
import com.tenantportal.model.Unit;
import com.tenantportal.repository.PropertyRepository;
import com.tenantportal.repository.UnitRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnitService {

    private final UnitRepository unitRepository;
    private final PropertyRepository propertyRepository;
    private final AuditService auditService;

    public List<Unit> findByPropertyId(Long propertyId) {
        return unitRepository.findByPropertyId(propertyId);
    }

    public Unit getByIdOrThrow(Long id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + id));
    }

    /**
     * Takes a propertyId and resolves the real, managed Property via
     * PropertyRepository rather than trusting a nested {"property":{"id":N}}
     * object deserialized straight from the request body. A JSON-deserialized
     * stub like that is a transient object as far as Hibernate's concerned —
     * saving a Unit that references one throws TransientPropertyValueException
     * at flush time, since the Unit->Property association has no cascade.
     */
    public Unit create(CreateUnitRequest request, String performedByClerkId) {
        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new EntityNotFoundException("Property not found: " + request.propertyId()));

        Unit unit = new Unit();
        unit.setUnitNumber(request.unitNumber());
        unit.setFloor(request.floor());
        unit.setProperty(property);
        unit.setMonthlyRent(request.monthlyRent());
        unit.setIsOccupied(false);

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
