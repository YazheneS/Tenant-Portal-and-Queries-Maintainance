package com.tenantportal.service;

import com.tenantportal.model.RentAgreement;
import com.tenantportal.repository.RentAgreementRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Module 6 — Rent Agreement Engine.
 *
 * checkUpcomingEscalations() runs daily, finds agreements whose
 * nextEscalationDate is exactly 30 days out, computes nextRentAmount, and
 * emails the tenant. applyDueEscalations() runs daily too, and on the actual
 * escalation date bumps baseRent to nextRentAmount and rolls the date
 * forward a year.
 */
@Service
@RequiredArgsConstructor
public class RentAgreementService {

    private final RentAgreementRepository rentAgreementRepository;
    private final AuditService auditService;
    private final EmailService emailService;

    public RentAgreement getActiveForUnit(Long unitId) {
        return rentAgreementRepository.findByUnitIdAndIsActiveTrue(unitId)
                .orElseThrow(() -> new EntityNotFoundException("No active rent agreement for unit: " + unitId));
    }

    public List<RentAgreement> findByTenantId(Long tenantId) {
        return rentAgreementRepository.findByTenantId(tenantId);
    }

    @Transactional
    public RentAgreement create(RentAgreement agreement, String performedByClerkId) {
        agreement.setIsActive(true);
        agreement.setNextEscalationDate(
                agreement.getStartDate().plusYears(1).withDayOfYear(
                        Math.min(agreement.getEscalationDayOfYear(),
                                agreement.getStartDate().plusYears(1).lengthOfYear())));
        agreement.setNextRentAmount(computeEscalatedRent(agreement.getBaseRent(), agreement.getEscalationPercent()));

        RentAgreement saved = rentAgreementRepository.save(agreement);
        auditService.log("RentAgreement", saved.getId(), "CREATED",
                null, saved.getBaseRent().toString(), performedByClerkId);
        return saved;
    }

    private BigDecimal computeEscalatedRent(BigDecimal baseRent, BigDecimal escalationPercent) {
        BigDecimal multiplier = BigDecimal.ONE.add(
                escalationPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        return baseRent.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /** Runs daily at 8 AM. Notifies tenants whose escalation is exactly 30 days away. */
    @Scheduled(cron = "0 0 8 * * *")
    public void checkUpcomingEscalations() {
        LocalDate thirtyDaysOut = LocalDate.now().plusDays(30);
        for (RentAgreement agreement : rentAgreementRepository.findByIsActiveTrueAndNextEscalationDateLessThanEqual(thirtyDaysOut)) {
            if (agreement.getNextEscalationDate() != null
                    && agreement.getNextEscalationDate().isEqual(thirtyDaysOut)) {
                String unitLabel = agreement.getUnit().getUnitNumber();
                emailService.notifyRentEscalationUpcoming(
                        agreement.getTenant().getEmail(), unitLabel, agreement.getNextRentAmount());
            }
        }
    }

    /** Runs daily at 3 AM. Applies escalations that are due today and rolls the date forward a year. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void applyDueEscalations() {
        LocalDate today = LocalDate.now();
        for (RentAgreement agreement : rentAgreementRepository.findByIsActiveTrueAndNextEscalationDateLessThanEqual(today)) {
            BigDecimal oldRent = agreement.getBaseRent();
            agreement.setBaseRent(agreement.getNextRentAmount());
            agreement.setNextEscalationDate(agreement.getNextEscalationDate().plusYears(1));
            agreement.setNextRentAmount(computeEscalatedRent(agreement.getBaseRent(), agreement.getEscalationPercent()));
            rentAgreementRepository.save(agreement);

            auditService.log("RentAgreement", agreement.getId(), "ESCALATION_APPLIED",
                    oldRent.toString(), agreement.getBaseRent().toString(), "system-scheduler");
        }
    }

    @Transactional
    public RentAgreement deactivate(Long id, String performedByClerkId) {
        RentAgreement agreement = rentAgreementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("RentAgreement not found: " + id));
        agreement.setIsActive(false);
        agreement.setEndDate(LocalDate.now());
        RentAgreement saved = rentAgreementRepository.save(agreement);
        auditService.log("RentAgreement", id, "DEACTIVATED", null, null, performedByClerkId);
        return saved;
    }
}
