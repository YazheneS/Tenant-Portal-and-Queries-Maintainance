package com.tenantportal.dto;

public record RaiseDisputeRequest(Long lineItemId, String reason, String evidencePhotoUrl) {
}
