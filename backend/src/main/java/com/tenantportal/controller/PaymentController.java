package com.tenantportal.controller;

import com.tenantportal.config.CurrentUser;
import com.tenantportal.dto.CreatePaymentOrderRequest;
import com.tenantportal.dto.CreateSplitPaymentRequest;
import com.tenantportal.dto.VerifyPaymentRequest;
import com.tenantportal.model.Payment;
import com.tenantportal.model.PaymentSplit;
import com.tenantportal.model.Tenant;
import com.tenantportal.service.PaymentService;
import com.tenantportal.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Module 5 — Payments. Lives under /api/tenant since only a tenant initiates
 * a payment on their own bill — SecurityConfig's existing
 * hasAnyAuthority("ROLE_OWNER", "ROLE_TENANT") on /api/tenant/** covers this
 * without needing a new matcher.
 */
@RestController
@RequestMapping("/api/tenant/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final TenantService tenantService;
    private final com.tenantportal.service.ReceiptService receiptService;
    private final CurrentUser currentUser;

    @PostMapping("/order")
    public Payment createOrder(@RequestBody CreatePaymentOrderRequest request) {
        Tenant tenant = tenantService.getByClerkUserIdOrThrow(currentUser.clerkUserId());
        return paymentService.createOrder(request.billId(), tenant);
    }

    @PostMapping("/split")
    public List<PaymentSplit> createSplitOrder(@RequestBody CreateSplitPaymentRequest request) {
        Tenant tenant = tenantService.getByClerkUserIdOrThrow(currentUser.clerkUserId());
        return paymentService.createSplitOrders(request.billId(), request.shares(), tenant);
    }

    /**
     * Called by the frontend immediately after Razorpay Checkout succeeds
     * client-side — this is where the server-side signature check actually
     * happens, so a payment is never trusted as real until this returns 200.
     */
    @PostMapping("/verify")
    public Payment verify(@RequestBody VerifyPaymentRequest request) {
        return paymentService.verifyAndRecordPayment(
                request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature());
    }

    @GetMapping("/history")
    public List<Payment> history() {
        Tenant tenant = tenantService.getByClerkUserIdOrThrow(currentUser.clerkUserId());
        return paymentService.findByTenantId(tenant.getId());
    }

    @GetMapping("/{paymentId}/receipt")
    public org.springframework.http.ResponseEntity<byte[]> downloadReceipt(@PathVariable Long paymentId) {
        byte[] pdf = receiptService.generateReceipt(paymentId);
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=receipt-" + paymentId + ".pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
