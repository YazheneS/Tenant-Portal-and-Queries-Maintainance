package com.tenantportal.repository;

import com.tenantportal.model.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    List<Vendor> findBySpecialization(Vendor.Specialization specialization);
}
