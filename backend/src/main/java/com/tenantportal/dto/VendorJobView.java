package com.tenantportal.dto;

import com.tenantportal.model.MaintenanceQuery;
import lombok.Value;

/**
 * What a vendor is allowed to see via their tokenized link — job details
 * only, no tenant contact info, no other jobs, no unit financials. This is
 * the projection VendorController returns instead of the raw entity.
 */
@Value
public class VendorJobView {
    Long id;
    String title;
    String description;
    String category;
    String priority;
    String status;
    String photoUrl;
    String unitNumber;
    String propertyAddress;

    public static VendorJobView from(MaintenanceQuery query) {
        return new VendorJobView(
                query.getId(),
                query.getTitle(),
                query.getDescription(),
                query.getCategory().name(),
                query.getPriority().name(),
                query.getStatus().name(),
                query.getPhotoUrl(),
                query.getUnit().getUnitNumber(),
                query.getUnit().getProperty().getAddress()
        );
    }
}
