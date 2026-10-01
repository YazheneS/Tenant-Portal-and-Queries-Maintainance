package com.tenantportal.service;

import com.tenantportal.model.Bill;
import com.tenantportal.model.BillDispute;
import com.tenantportal.model.BillLineItem;
import com.tenantportal.model.Tenant;
import com.tenantportal.repository.BillDisputeRepository;
import com.tenantportal.repository.BillLineItemRepository;
import com.tenantportal.repository.BillRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Module 4 — Bill Dispute state machine: RAISED -> UNDER_REVIEW -> ACCEPTED/REJECTED.
 * Master plan also lists a REVISED step after ACCEPTED (the bill gets
 * corrected) — that's modeled here as: on ACCEPTED, the disputed line item's
 * amount is zeroed and the Bill's totalAmount recalculated, rather than as a
 * separate status, since "revised" is a side-effect of acceptance, not a
 * distinct state the dispute itself sits in.
 */
@Service
@RequiredArgsConstructor
public class BillDisputeService {

    private final BillDisputeRepository billDisputeRepository;
    private final BillLineItemRepository billLineItemRepository;
    private final BillRepository billRepository;
    private final AuditService auditService;
    private final EmailService emailService;

    public List<BillDispute> findByTenantId(Long tenantId) {
        return billDisputeRepository.findByTenantId(tenantId);
    }

    public List<BillDispute> findByStatus(BillDispute.Status status) {
        return billDisputeRepository.findByStatus(status);
    }

    @Transactional
    public BillDispute raise(Tenant tenant, Long billId, Long lineItemId, String reason, String evidencePhotoUrl) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new EntityNotFoundException("Bill not found: " + billId));
        BillLineItem lineItem = billLineItemRepository.findById(lineItemId)
                .orElseThrow(() -> new EntityNotFoundException("Line item not found: " + lineItemId));

        BillDispute dispute = new BillDispute();
        dispute.setBill(bill);
        dispute.setLineItem(lineItem);
        dispute.setTenant(tenant);
        dispute.setReason(reason);
        dispute.setEvidencePhotoUrl(evidencePhotoUrl);
        dispute.setStatus(BillDispute.Status.RAISED);
        BillDispute saved = billDisputeRepository.save(dispute);

        bill.setStatus(Bill.Status.DISPUTED);
        billRepository.save(bill);

        auditService.log("BillDispute", saved.getId(), "RAISED", null, reason, tenant.getClerkUserId());
        emailService.notifyOwnerDisputeRaised(bill, saved);
        return saved;
    }

    @Transactional
    public BillDispute markUnderReview(Long disputeId, String performedByClerkId) {
        BillDispute dispute = getByIdOrThrow(disputeId);
        dispute.setStatus(BillDispute.Status.UNDER_REVIEW);
        BillDispute saved = billDisputeRepository.save(dispute);
        auditService.log("BillDispute", disputeId, "UNDER_REVIEW", null, null, performedByClerkId);
        emailService.notifyTenantDisputeStatusChanged(dispute);
        return saved;
    }

    @Transactional
    public BillDispute resolve(Long disputeId, BillDispute.Status resolution, String ownerResponse,
                                String performedByClerkId) {
        if (resolution != BillDispute.Status.ACCEPTED && resolution != BillDispute.Status.REJECTED) {
            throw new IllegalArgumentException("Resolution must be ACCEPTED or REJECTED");
        }

        BillDispute dispute = getByIdOrThrow(disputeId);
        dispute.setStatus(resolution);
        dispute.setOwnerResponse(ownerResponse);
        dispute.setResolvedAt(java.time.LocalDateTime.now());

        if (resolution == BillDispute.Status.ACCEPTED) {
            // "Revised": zero out the disputed line item and recalculate the bill total.
            BillLineItem lineItem = dispute.getLineItem();
            Bill bill = dispute.getBill();
            bill.setTotalAmount(bill.getTotalAmount().subtract(lineItem.getAmount()));
            lineItem.setAmount(java.math.BigDecimal.ZERO);
            billLineItemRepository.save(lineItem);
            bill.setStatus(Bill.Status.CONFIRMED);
            billRepository.save(bill);
        } else {
            dispute.getBill().setStatus(Bill.Status.CONFIRMED);
            billRepository.save(dispute.getBill());
        }

        BillDispute saved = billDisputeRepository.save(dispute);
        auditService.log("BillDispute", disputeId, "RESOLVED_" + resolution.name(),
                null, ownerResponse, performedByClerkId);
        emailService.notifyTenantDisputeStatusChanged(saved);
        return saved;
    }

    private BillDispute getByIdOrThrow(Long id) {
        return billDisputeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("BillDispute not found: " + id));
    }
}
