package com.tenantportal.controller;

import com.tenantportal.config.CurrentUser;
import com.tenantportal.dto.AddLineItemRequest;
import com.tenantportal.dto.AssignVendorRequest;
import com.tenantportal.dto.CreateBillRequest;
import com.tenantportal.dto.ResolveDisputeRequest;
import com.tenantportal.model.Bill;
import com.tenantportal.model.BillDispute;
import com.tenantportal.model.BillLineItem;
import com.tenantportal.model.MaintenanceQuery;
import com.tenantportal.model.Property;
import com.tenantportal.model.RentAgreement;
import com.tenantportal.model.Unit;
import com.tenantportal.model.Vendor;
import com.tenantportal.service.BillDisputeService;
import com.tenantportal.service.BillService;
import com.tenantportal.service.MaintenanceQueryService;
import com.tenantportal.service.PropertyService;
import com.tenantportal.service.RentAgreementService;
import com.tenantportal.service.UnitService;
import com.tenantportal.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Owner-only surface. SecurityConfig already restricts /api/admin/** to
 * ROLE_OWNER, so no per-method role check is needed here — Spring Security
 * rejects anything that gets this far with the wrong role before the
 * controller method even runs.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final PropertyService propertyService;
    private final UnitService unitService;
    private final VendorService vendorService;
    private final MaintenanceQueryService maintenanceQueryService;
    private final BillService billService;
    private final BillDisputeService billDisputeService;
    private final RentAgreementService rentAgreementService;
    private final com.tenantportal.service.TenantService tenantService;
    private final CurrentUser currentUser;

    // --- Properties ---

    @GetMapping("/properties")
    public List<Property> myProperties() {
        return propertyService.findAllForOwner(currentUser.clerkUserId());
    }

    @PostMapping("/properties")
    public Property createProperty(@RequestBody Property property) {
        return propertyService.create(property, currentUser.clerkUserId());
    }

    @PutMapping("/properties/{id}")
    public Property updateProperty(@PathVariable Long id, @RequestBody Property updates) {
        return propertyService.update(id, updates, currentUser.clerkUserId());
    }

    @DeleteMapping("/properties/{id}")
    public void deleteProperty(@PathVariable Long id) {
        propertyService.delete(id, currentUser.clerkUserId());
    }

    // --- Units ---

    @GetMapping("/properties/{propertyId}/units")
    public List<Unit> unitsForProperty(@PathVariable Long propertyId) {
        return unitService.findByPropertyId(propertyId);
    }

    @PostMapping("/units")
    public Unit createUnit(@RequestBody com.tenantportal.dto.CreateUnitRequest request) {
        return unitService.create(request, currentUser.clerkUserId());
    }

    @PutMapping("/units/{id}")
    public Unit updateUnit(@PathVariable Long id, @RequestBody Unit updates) {
        return unitService.update(id, updates, currentUser.clerkUserId());
    }

    // --- Vendors ---

    @GetMapping("/vendors")
    public List<Vendor> vendors() {
        return vendorService.findAll();
    }

    @PostMapping("/vendors")
    public Vendor createVendor(@RequestBody Vendor vendor) {
        return vendorService.create(vendor);
    }

    // --- Maintenance queries: the Owner's view across all tenants ---

    @GetMapping("/maintenance-queries")
    public List<MaintenanceQuery> openQueries() {
        return maintenanceQueryService.findByStatus(MaintenanceQuery.Status.OPEN);
    }

    @PostMapping("/maintenance-queries/{id}/assign-vendor")
    public MaintenanceQuery assignVendor(@PathVariable Long id, @RequestBody AssignVendorRequest request) {
        return maintenanceQueryService.assignVendor(id, request.vendorId(), currentUser.clerkUserId());
    }

    @PostMapping("/maintenance-queries/{id}/close")
    public MaintenanceQuery closeQuery(@PathVariable Long id) {
        return maintenanceQueryService.close(id, currentUser.clerkUserId());
    }

    // --- Billing (Module 4) ---

    @PostMapping("/bills")
    public Bill createBill(@RequestBody CreateBillRequest request) {
        return billService.create(request, currentUser.clerkUserId());
    }

    @GetMapping("/bills/{id}")
    public Bill getBill(@PathVariable Long id) {
        return billService.getByIdOrThrow(id);
    }

    @PostMapping("/bills/{id}/mark-paid")
    public Bill markPaid(@PathVariable Long id, @RequestParam java.math.BigDecimal amount) {
        return billService.markPaid(id, amount, currentUser.clerkUserId());
    }

    @PostMapping("/bills/{id}/line-items")
    public BillLineItem addLineItem(@PathVariable Long id, @RequestBody AddLineItemRequest request) {
        return billService.addLineItem(id, request.type(), request.description(),
                request.amount(), request.units(), request.ratePerUnit(), currentUser.clerkUserId());
    }

    @GetMapping("/disputes")
    public List<BillDispute> openDisputes() {
        return billDisputeService.findByStatus(BillDispute.Status.RAISED);
    }

    @PostMapping("/disputes/{id}/review")
    public BillDispute reviewDispute(@PathVariable Long id) {
        return billDisputeService.markUnderReview(id, currentUser.clerkUserId());
    }

    @PostMapping("/disputes/{id}/resolve")
    public BillDispute resolveDispute(@PathVariable Long id, @RequestBody ResolveDisputeRequest request) {
        return billDisputeService.resolve(id, request.status(), request.ownerResponse(), currentUser.clerkUserId());
    }

    // --- Rent Agreements (Module 6) ---

    @PostMapping("/rent-agreements")
    public RentAgreement createRentAgreement(@RequestBody com.tenantportal.dto.CreateRentAgreementRequest request) {
        return rentAgreementService.create(request, currentUser.clerkUserId());
    }

    @GetMapping("/rent-agreements/unit/{unitId}")
    public RentAgreement activeAgreementForUnit(@PathVariable Long unitId) {
        return rentAgreementService.getActiveForUnit(unitId);
    }

    // --- Tenant occupancy ---

    @PostMapping("/tenants/{tenantId}/assign-unit/{unitId}")
    public com.tenantportal.model.Tenant assignTenantToUnit(@PathVariable Long tenantId, @PathVariable Long unitId) {
        return tenantService.assignToUnit(tenantId, unitId, currentUser.clerkUserId());
    }

    @PostMapping("/tenants/{tenantId}/move-out")
    public com.tenantportal.model.Tenant moveOutTenant(@PathVariable Long tenantId,
                                                         @RequestParam(defaultValue = "0") java.math.BigDecimal outstandingAmount,
                                                         @RequestParam(required = false) String notes) {
        return tenantService.moveOut(tenantId, outstandingAmount, notes, currentUser.clerkUserId());
    }
}
