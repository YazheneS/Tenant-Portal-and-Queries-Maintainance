package com.tenantportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling turns on BillService.generateMonthlyRentBills() and
// RentAgreementService's escalation check — both use @Scheduled and do
// nothing without this.
@SpringBootApplication
@EnableScheduling
public class TenantPortalApplication {
    public static void main(String[] args) {
        SpringApplication.run(TenantPortalApplication.class, args);
    }
}
