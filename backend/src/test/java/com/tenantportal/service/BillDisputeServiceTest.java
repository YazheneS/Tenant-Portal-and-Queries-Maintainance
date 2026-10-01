package com.tenantportal.service;

import com.tenantportal.model.*;
import com.tenantportal.repository.BillDisputeRepository;
import com.tenantportal.repository.BillLineItemRepository;
import com.tenantportal.repository.BillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillDisputeServiceTest {

    @Mock BillDisputeRepository billDisputeRepository;
    @Mock BillLineItemRepository billLineItemRepository;
    @Mock BillRepository billRepository;
    @Mock AuditService auditService;
    @Mock EmailService emailService;

    @InjectMocks BillDisputeService service;

    @Test
    void raise_setsStatusRaisedAndMarksBillDisputed() {
        Bill bill = new Bill();
        bill.setId(1L);
        bill.setStatus(Bill.Status.GENERATED);
        BillLineItem lineItem = new BillLineItem();
        lineItem.setId(2L);
        Tenant tenant = new Tenant();
        tenant.setId(3L);

        when(billRepository.findById(1L)).thenReturn(Optional.of(bill));
        when(billLineItemRepository.findById(2L)).thenReturn(Optional.of(lineItem));
        when(billDisputeRepository.save(any(BillDispute.class))).thenAnswer(inv -> inv.getArgument(0));

        BillDispute result = service.raise(tenant, 1L, 2L, "Overcharged", null);

        assertEquals(BillDispute.Status.RAISED, result.getStatus());
        assertEquals(Bill.Status.DISPUTED, bill.getStatus());
        verify(emailService).notifyOwnerDisputeRaised(bill, result);
    }

    @Test
    void resolve_rejectsInvalidResolutionStatus() {
        assertThrows(IllegalArgumentException.class, () ->
                service.resolve(1L, BillDispute.Status.RAISED, "no", "clerk_id"));
        verifyNoInteractions(billDisputeRepository);
    }

    @Test
    void resolve_acceptedZeroesLineItemAndRecalculatesBillTotal() {
        BillLineItem lineItem = new BillLineItem();
        lineItem.setAmount(BigDecimal.valueOf(500));

        Bill bill = new Bill();
        bill.setTotalAmount(BigDecimal.valueOf(2000));

        BillDispute dispute = new BillDispute();
        dispute.setId(9L);
        dispute.setLineItem(lineItem);
        dispute.setBill(bill);
        dispute.setTenant(new Tenant());

        when(billDisputeRepository.findById(9L)).thenReturn(Optional.of(dispute));
        when(billDisputeRepository.save(any(BillDispute.class))).thenAnswer(inv -> inv.getArgument(0));

        BillDispute result = service.resolve(9L, BillDispute.Status.ACCEPTED, "Confirmed error", "owner_clerk_id");

        assertEquals(BillDispute.Status.ACCEPTED, result.getStatus());
        assertEquals(BigDecimal.ZERO, lineItem.getAmount());
        assertEquals(BigDecimal.valueOf(1500), bill.getTotalAmount());
        assertEquals(Bill.Status.CONFIRMED, bill.getStatus());
    }
}
