package com.tenantportal.service;

import com.tenantportal.model.Vendor;
import com.tenantportal.repository.VendorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;

    public List<Vendor> findAll() {
        return vendorRepository.findAll();
    }

    public List<Vendor> findBySpecialization(Vendor.Specialization specialization) {
        return vendorRepository.findBySpecialization(specialization);
    }

    public Vendor getByIdOrThrow(Long id) {
        return vendorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vendor not found: " + id));
    }

    public Vendor create(Vendor vendor) {
        return vendorRepository.save(vendor);
    }

    public void incrementJobsCompleted(Long vendorId) {
        Vendor vendor = getByIdOrThrow(vendorId);
        vendor.setTotalJobsCompleted(vendor.getTotalJobsCompleted() + 1);
        vendorRepository.save(vendor);
    }
}
