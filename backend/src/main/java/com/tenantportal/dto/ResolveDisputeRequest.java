package com.tenantportal.dto;

import com.tenantportal.model.BillDispute;

public record ResolveDisputeRequest(BillDispute.Status status, String ownerResponse) {
}
