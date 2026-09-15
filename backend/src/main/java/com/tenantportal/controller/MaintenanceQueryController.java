package com.tenantportal.controller;

import com.tenantportal.model.MaintenanceQuery;
import com.tenantportal.model.MaintenanceQuery.Status;
import com.tenantportal.service.MaintenanceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/queries")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class MaintenanceQueryController {

    private final MaintenanceQueryService service;

    @PostMapping("/tenant/{tenantId}")
    public ResponseEntity<MaintenanceQuery> createQuery(
            @PathVariable Long tenantId,
            @RequestBody MaintenanceQuery query) {
        return ResponseEntity.ok(service.createQuery(tenantId, query));
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<MaintenanceQuery>> getTenantQueries(
            @PathVariable Long tenantId) {
        return ResponseEntity.ok(service.getQueriesByTenant(tenantId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<MaintenanceQuery>> getByStatus(
            @PathVariable Status status) {
        return ResponseEntity.ok(service.getQueriesByStatus(status));
    }

    @PatchMapping("/{queryId}/status")
    public ResponseEntity<MaintenanceQuery> updateStatus(
            @PathVariable Long queryId,
            @RequestParam Status newStatus) {
        return ResponseEntity.ok(service.updateStatus(queryId, newStatus));
    }

    @DeleteMapping("/{queryId}")
    public ResponseEntity<Void> deleteQuery(@PathVariable Long queryId) {
        service.deleteQuery(queryId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countByStatus(@RequestParam Status status) {
        return ResponseEntity.ok(service.countByStatus(status));
    }
}
