package com.tenantportal.controller;

import com.tenantportal.config.CurrentUser;
import com.tenantportal.dto.RaiseDisputeRequest;
import com.tenantportal.model.Bill;
import com.tenantportal.model.BillDispute;
import com.tenantportal.model.MaintenanceQuery;
import com.tenantportal.model.RentAgreement;
import com.tenantportal.model.Tenant;
import com.tenantportal.service.BillDisputeService;
import com.tenantportal.service.BillService;
import com.tenantportal.service.CloudinaryService;
import com.tenantportal.service.MaintenanceQueryService;
import com.tenantportal.service.RentAgreementService;
import com.tenantportal.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Tenant's own self-service surface. Everything here is scoped to the
 * currently authenticated tenant (resolved via CurrentUser -> clerkUserId ->
 * TenantRepository) — a tenant can never pass someone else's id and see
 * their data, since we never trust an id from the request body/path for
 * "whose data is this."
 */
@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;
    private final MaintenanceQueryService maintenanceQueryService;
    private final BillService billService;
    private final BillDisputeService billDisputeService;
    private final RentAgreementService rentAgreementService;
    private final CloudinaryService cloudinaryService;
    private final CurrentUser currentUser;

    @GetMapping("/me")
    public Tenant me() {
        return tenantService.getByClerkUserIdOrThrow(currentUser.clerkUserId());
    }

    @GetMapping("/maintenance-queries")
    public List<MaintenanceQuery> myQueries() {
        Tenant tenant = tenantService.getByClerkUserIdOrThrow(currentUser.clerkUserId());
        return maintenanceQueryService.findByTenantId(tenant.getId());
    }

    @PostMapping("/maintenance-queries")
    public MaintenanceQuery raiseQuery(@RequestBody MaintenanceQuery query) {
        Tenant tenant = tenantService.getByClerkUserIdOrThrow(currentUser.clerkUserId());
        return maintenanceQueryService.raise(tenant, query);
    }

    /** Uploads a complaint photo to Cloudinary, returns the URL to attach via PATCH before/after raising. */
    @PostMapping("/maintenance-queries/photo")
    public Map<String, String> uploadComplaintPhoto(@RequestParam("file") MultipartFile file) {
        String url = cloudinaryService.uploadComplaintPhoto(file);
        return Map.of("url", url);
    }

    // --- Billing (Module 4) ---

    @GetMapping("/bills")
    public List<Bill> myBills() {
        Tenant tenant = tenantService.getByClerkUserIdOrThrow(currentUser.clerkUserId());
        return billService.findByTenantId(tenant.getId());
    }

    @GetMapping("/bills/{id}")
    public Bill billDetail(@PathVariable Long id) {
        return billService.getByIdOrThrow(id);
    }

    @GetMapping("/bills/{id}/line-items")
    public List<com.tenantportal.model.BillLineItem> billLineItems(@PathVariable Long id) {
        return billService.lineItemsFor(id);
    }

    @PostMapping("/bills/{id}/dispute")
    public BillDispute raiseDispute(@PathVariable Long id, @RequestBody RaiseDisputeRequest request) {
        Tenant tenant = tenantService.getByClerkUserIdOrThrow(currentUser.clerkUserId());
        return billDisputeService.raise(tenant, id, request.lineItemId(), request.reason(), request.evidencePhotoUrl());
    }

    @GetMapping("/disputes")
    public List<BillDispute> myDisputes() {
        Tenant tenant = tenantService.getByClerkUserIdOrThrow(currentUser.clerkUserId());
        return billDisputeService.findByTenantId(tenant.getId());
    }

    // --- Rent agreement (Module 6) ---

    @GetMapping("/rent-agreement")
    public List<RentAgreement> myRentAgreements() {
        Tenant tenant = tenantService.getByClerkUserIdOrThrow(currentUser.clerkUserId());
        return rentAgreementService.findByTenantId(tenant.getId());
    }
}
