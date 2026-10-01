package com.tenantportal.repository;

import com.tenantportal.model.RentAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RentAgreementRepository extends JpaRepository<RentAgreement, Long> {
    Optional<RentAgreement> findByUnitIdAndIsActiveTrue(Long unitId);
    List<RentAgreement> findByTenantId(Long tenantId);
    List<RentAgreement> findByIsActiveTrue();
    List<RentAgreement> findByIsActiveTrueAndNextEscalationDateLessThanEqual(LocalDate date);
}
