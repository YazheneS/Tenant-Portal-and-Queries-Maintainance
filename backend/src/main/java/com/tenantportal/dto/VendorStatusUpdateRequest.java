package com.tenantportal.dto;

import com.tenantportal.model.MaintenanceQuery;

public record VendorStatusUpdateRequest(MaintenanceQuery.Status status, String resolutionNotes) {
}
