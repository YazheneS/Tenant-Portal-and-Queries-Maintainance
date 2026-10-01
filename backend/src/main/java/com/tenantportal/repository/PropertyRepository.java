package com.tenantportal.repository;

import com.tenantportal.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {
    List<Property> findByOwnerClerkId(String ownerClerkId);
}
