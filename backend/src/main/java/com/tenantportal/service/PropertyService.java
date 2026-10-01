package com.tenantportal.service;

import com.tenantportal.model.Property;
import com.tenantportal.repository.PropertyRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final AuditService auditService;

    public List<Property> findAllForOwner(String ownerClerkId) {
        return propertyRepository.findByOwnerClerkId(ownerClerkId);
    }

    public Property getByIdOrThrow(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Property not found: " + id));
    }

    public Property create(Property property, String ownerClerkId) {
        property.setOwnerClerkId(ownerClerkId);
        Property saved = propertyRepository.save(property);
        auditService.log("Property", saved.getId(), "CREATED", null, saved.getName(), ownerClerkId);
        return saved;
    }

    public Property update(Long id, Property updates, String performedByClerkId) {
        Property existing = getByIdOrThrow(id);
        existing.setName(updates.getName());
        existing.setAddress(updates.getAddress());
        Property saved = propertyRepository.save(existing);
        auditService.log("Property", id, "UPDATED", null, saved.getName(), performedByClerkId);
        return saved;
    }

    public void delete(Long id, String performedByClerkId) {
        getByIdOrThrow(id);
        propertyRepository.deleteById(id);
        auditService.log("Property", id, "DELETED", null, null, performedByClerkId);
    }
}
