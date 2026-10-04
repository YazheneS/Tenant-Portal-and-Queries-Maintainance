package com.tenantportal.service;

import com.tenantportal.dto.CreateBillRequest;
import com.tenantportal.model.Bill;
import com.tenantportal.model.BillLineItem;
import com.tenantportal.model.RentAgreement;
import com.tenantportal.model.Tenant;
import com.tenantportal.model.Unit;
import com.tenantportal.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Module 4 — Billing Engine.
 *
 * generateMonthlyRentBills() is the @Scheduled piece from the master plan:
 * runs once a month, auto-creates a bill with a single RENT line item for
 * every unit that has an active RentAgreement. Owners then add
 * electricity/water/other line items manually via addLineItem() before
 * marking the bill ready for the tenant.
 */
@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final BillLineItemRepository billLineItemRepository;
    private final UnitRepository unitRepository;
    private final TenantRepository tenantRepository;
    private final RentAgreementRepository rentAgreementRepository;
    private final AuditService auditService;

    public List<Bill> findByTenantId(Long tenantId) {
        return billRepository.findByTenantId(tenantId);
    }

    public List<Bill> findAll() {
        return billRepository.findAll();
    }

    public Bill getByIdOrThrow(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bill not found: " + id));
    }

    public List<BillLineItem> lineItemsFor(Long billId) {
        return billLineItemRepository.findByBillId(billId);
    }

    @Transactional
    public Bill create(CreateBillRequest request, String performedByClerkId) {
        Unit unit = unitRepository.findById(request.unitId())
                .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + request.unitId()));
        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + request.tenantId()));

        Bill bill = new Bill();
        bill.setUnit(unit);
        bill.setTenant(tenant);
        bill.setBillMonth(request.billMonth());
        bill.setBillYear(request.billYear());
        bill.setDueDate(request.dueDate());
        bill.setStatus(Bill.Status.GENERATED);
        bill.setTotalAmount(BigDecimal.ZERO);
        Bill saved = billRepository.save(bill);

        BigDecimal total = BigDecimal.ZERO;
        if (request.lineItems() != null) {
            for (var itemReq : request.lineItems()) {
                BillLineItem item = new BillLineItem();
                item.setBill(saved);
                item.setType(itemReq.type());
                item.setDescription(itemReq.description());
                item.setAmount(itemReq.amount());
                item.setUnits(itemReq.units());
                item.setRatePerUnit(itemReq.ratePerUnit());
                billLineItemRepository.save(item);
                total = total.add(itemReq.amount());
            }
        }
        saved.setTotalAmount(total);
        Bill finalBill = billRepository.save(saved);

        auditService.log("Bill", finalBill.getId(), "CREATED", null,
                total.toString(), performedByClerkId);
        return finalBill;
    }

    @Transactional
    public BillLineItem addLineItem(Long billId, BillLineItem.Type type, String description,
                                     BigDecimal amount, Double units, BigDecimal ratePerUnit,
                                     String performedByClerkId) {
        Bill bill = getByIdOrThrow(billId);

        BillLineItem item = new BillLineItem();
        item.setBill(bill);
        item.setType(type);
        item.setDescription(description);
        item.setAmount(amount);
        item.setUnits(units);
        item.setRatePerUnit(ratePerUnit);
        BillLineItem saved = billLineItemRepository.save(item);

        bill.setTotalAmount(bill.getTotalAmount().add(amount));
        billRepository.save(bill);

        auditService.log("BillLineItem", saved.getId(), "ADDED",
                null, amount.toString(), performedByClerkId);
        return saved;
    }

    @Transactional
    public Bill markPaid(Long billId, BigDecimal amountPaid, String performedByClerkId) {
        Bill bill = getByIdOrThrow(billId);
        Bill.Status oldStatus = bill.getStatus();
        bill.setPaidAmount(bill.getPaidAmount().add(amountPaid));
        if (bill.getPaidAmount().compareTo(bill.getTotalAmount()) >= 0) {
            bill.setStatus(Bill.Status.PAID);
        }
        Bill saved = billRepository.save(bill);
        auditService.log("Bill", billId, "PAYMENT_APPLIED",
                oldStatus.name(), saved.getStatus().name(), performedByClerkId);
        return saved;
    }

    /**
     * Runs at 2 AM on the 1st of every month. Cron: sec min hour day month weekday.
     * Marks any bill still unpaid past its due date as OVERDUE first, then
     * generates this month's rent bill for every unit under an active
     * RentAgreement that doesn't already have one for this month/year.
     */
    @Scheduled(cron = "0 0 2 1 * *")
    @Transactional
    public void generateMonthlyRentBills() {
        flagOverdueBills();

        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        for (RentAgreement agreement : rentAgreementRepository.findByIsActiveTrue()) {
            Unit unit = agreement.getUnit();
            Tenant tenant = agreement.getTenant();

            boolean alreadyExists = !billRepository
                    .findByUnitIdAndBillMonthAndBillYear(unit.getId(), month, year)
                    .isEmpty();
            if (alreadyExists) {
                continue;
            }

            Bill bill = new Bill();
            bill.setUnit(unit);
            bill.setTenant(tenant);
            bill.setBillMonth(month);
            bill.setBillYear(year);
            bill.setDueDate(now.withDayOfMonth(Math.min(10, now.lengthOfMonth())));
            bill.setStatus(Bill.Status.GENERATED);
            bill.setTotalAmount(agreement.getBaseRent());
            Bill savedBill = billRepository.save(bill);

            BillLineItem rentLine = new BillLineItem();
            rentLine.setBill(savedBill);
            rentLine.setType(BillLineItem.Type.RENT);
            rentLine.setDescription("Rent for " + month + "/" + year);
            rentLine.setAmount(agreement.getBaseRent());
            billLineItemRepository.save(rentLine);

            auditService.log("Bill", savedBill.getId(), "AUTO_GENERATED_MONTHLY_RENT",
                    null, agreement.getBaseRent().toString(), "system-scheduler");
        }
    }

    @Transactional
    public void flagOverdueBills() {
        LocalDate today = LocalDate.now();
        for (Bill bill : billRepository.findByStatus(Bill.Status.GENERATED)) {
            if (bill.getDueDate() != null && bill.getDueDate().isBefore(today)) {
                bill.setStatus(Bill.Status.OVERDUE);
                billRepository.save(bill);
                auditService.log("Bill", bill.getId(), "MARKED_OVERDUE",
                        Bill.Status.GENERATED.name(), Bill.Status.OVERDUE.name(), "system-scheduler");
            }
        }
    }
}
