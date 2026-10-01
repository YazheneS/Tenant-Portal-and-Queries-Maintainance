package com.tenantportal.service;

import com.tenantportal.model.MaintenanceQuery;
import com.tenantportal.model.Tenant;
import com.tenantportal.model.Unit;
import com.tenantportal.model.Vendor;
import com.tenantportal.repository.MaintenanceQueryRepository;
import com.tenantportal.repository.VendorRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceQueryServiceTest {

    @Mock MaintenanceQueryRepository maintenanceQueryRepository;
    @Mock VendorRepository vendorRepository;
    @Mock AuditService auditService;
    @Mock EmailService emailService;

    @InjectMocks MaintenanceQueryService service;

    @Test
    void raise_throwsWhenTenantHasNoUnit() {
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setUnit(null); // not assigned yet

        MaintenanceQuery query = new MaintenanceQuery();

        assertThrows(IllegalStateException.class, () -> service.raise(tenant, query));
        verifyNoInteractions(maintenanceQueryRepository);
    }

    @Test
    void raise_succeedsAndSetsOpenStatus() {
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setClerkUserId("clerk_123");
        Unit unit = new Unit();
        unit.setId(5L);
        tenant.setUnit(unit);

        MaintenanceQuery query = new MaintenanceQuery();
        query.setTitle("Leaky faucet");

        when(maintenanceQueryRepository.save(any(MaintenanceQuery.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MaintenanceQuery result = service.raise(tenant, query);

        assertEquals(MaintenanceQuery.Status.OPEN, result.getStatus());
        assertEquals(unit, result.getUnit());
        assertEquals(tenant, result.getTenant());
        verify(auditService).log(eq("MaintenanceQuery"), isNull(), eq("RAISED"), isNull(), eq("Leaky faucet"), eq("clerk_123"));
    }

    @Test
    void assignVendor_generatesTokenWithFutureExpiry() {
        MaintenanceQuery query = new MaintenanceQuery();
        query.setId(10L);
        Vendor vendor = new Vendor();
        vendor.setId(2L);

        when(maintenanceQueryRepository.findById(10L)).thenReturn(Optional.of(query));
        when(vendorRepository.findById(2L)).thenReturn(Optional.of(vendor));
        when(maintenanceQueryRepository.save(any(MaintenanceQuery.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MaintenanceQuery result = service.assignVendor(10L, 2L, "owner_clerk_id");

        assertEquals(MaintenanceQuery.Status.ASSIGNED, result.getStatus());
        assertNotNull(result.getVendorAccessToken());
        assertTrue(result.getVendorTokenExpiresAt().isAfter(LocalDateTime.now()));
        assertEquals(vendor, result.getAssignedVendor());
    }

    @Test
    void getByValidVendorToken_throwsWhenExpiredOrUnknown() {
        when(maintenanceQueryRepository.findByVendorAccessTokenAndVendorTokenExpiresAtAfter(anyString(), any()))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.getByValidVendorToken("bad-token"));
    }
}
