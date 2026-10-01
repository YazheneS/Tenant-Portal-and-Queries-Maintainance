package com.tenantportal.controller;

import com.tenantportal.dto.VendorJobView;
import com.tenantportal.dto.VendorStatusUpdateRequest;
import com.tenantportal.model.MaintenanceQuery;
import com.tenantportal.service.MaintenanceQueryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * No Clerk auth here by design — this whole controller is permitAll() in
 * SecurityConfig. Access control is the token itself: every endpoint takes
 * the vendorAccessToken as a path variable and MaintenanceQueryService
 * rejects anything expired or unknown. Rate limiting this controller with
 * Bucket4J is still on the list (per the original auth design note) since a
 * UUID token-lookup endpoint being public is exactly the kind of thing worth
 * throttling against brute-force.
 */
@RestController
@RequestMapping("/api/vendor")
@RequiredArgsConstructor
public class VendorController {

    private final MaintenanceQueryService maintenanceQueryService;

    @GetMapping("/jobs/{token}")
    public VendorJobView getJob(@PathVariable String token) {
        try {
            MaintenanceQuery query = maintenanceQueryService.getByValidVendorToken(token);
            return VendorJobView.from(query);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid or expired link");
        }
    }

    @PatchMapping("/jobs/{token}/status")
    public ResponseEntity<VendorJobView> updateStatus(@PathVariable String token,
                                                        @RequestBody VendorStatusUpdateRequest request) {
        try {
            MaintenanceQuery updated = maintenanceQueryService.updateStatusByVendorToken(
                    token, request.status(), request.resolutionNotes());
            return ResponseEntity.ok(VendorJobView.from(updated));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid or expired link");
        }
    }
}
