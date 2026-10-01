package com.tenantportal.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.tenantportal.model.Bill;
import com.tenantportal.model.Payment;
import com.tenantportal.model.PaymentSplit;
import com.tenantportal.model.Tenant;
import com.tenantportal.repository.BillRepository;
import com.tenantportal.repository.PaymentRepository;
import com.tenantportal.repository.PaymentSplitRepository;
import com.tenantportal.repository.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Module 5 — Payments.
 *
 * Needs razorpay.key-id and razorpay.key-secret set (Doppler, not committed).
 * Test mode keys from the Razorpay dashboard work fine for development —
 * they start with rzp_test_ instead of rzp_live_.
 *
 * Flow: createOrder() hits Razorpay to get a razorpay_order_id, frontend
 * opens Razorpay Checkout with it, Razorpay calls back to the frontend with
 * a payment id + signature, frontend sends those to verifyAndRecordPayment()
 * which checks the signature server-side before ever marking anything paid.
 * Never trust a "payment succeeded" claim from the frontend without this
 * signature check — it's the only part of this flow an attacker can't fake.
 */
@Service
@Slf4j
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final String keySecret;
    private final PaymentRepository paymentRepository;
    private final PaymentSplitRepository paymentSplitRepository;
    private final BillRepository billRepository;
    private final TenantRepository tenantRepository;
    private final BillService billService;
    private final AuditService auditService;

    public PaymentService(@Value("${razorpay.key-id:}") String keyId,
                           @Value("${razorpay.key-secret:}") String keySecret,
                           PaymentRepository paymentRepository,
                           PaymentSplitRepository paymentSplitRepository,
                           BillRepository billRepository,
                           TenantRepository tenantRepository,
                           BillService billService,
                           AuditService auditService) throws RazorpayException {
        this.razorpayClient = new RazorpayClient(keyId, keySecret);
        this.keySecret = keySecret;
        this.paymentRepository = paymentRepository;
        this.paymentSplitRepository = paymentSplitRepository;
        this.billRepository = billRepository;
        this.tenantRepository = tenantRepository;
        this.billService = billService;
        this.auditService = auditService;
    }

    @Transactional
    public Payment createOrder(Long billId, Tenant initiatedBy) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new EntityNotFoundException("Bill not found: " + billId));

        BigDecimal amountDue = bill.getTotalAmount().subtract(bill.getPaidAmount());
        if (amountDue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bill already fully paid");
        }

        try {
            JSONObject orderRequest = new JSONObject();
            // Razorpay wants the amount in paise (smallest currency unit).
            orderRequest.put("amount", amountDue.multiply(BigDecimal.valueOf(100)).intValue());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "bill_" + billId + "_" + System.currentTimeMillis());

            com.razorpay.Order order = razorpayClient.orders.create(orderRequest);

            Payment payment = new Payment();
            payment.setBill(bill);
            payment.setTenant(initiatedBy);
            payment.setRazorpayOrderId(order.get("id"));
            payment.setAmount(amountDue);
            payment.setStatus(Payment.Status.INITIATED);
            Payment saved = paymentRepository.save(payment);

            auditService.log("Payment", saved.getId(), "ORDER_CREATED",
                    null, amountDue.toString(), initiatedBy.getClerkUserId());
            return saved;
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not create payment order");
        }
    }

    /**
     * Same as createOrder, but splits the amount across multiple tenants for
     * a shared flat. Creates one Razorpay order per tenant's share so each
     * person pays their own slice independently.
     */
    @Transactional
    public List<PaymentSplit> createSplitOrders(Long billId, List<com.tenantportal.dto.CreateSplitPaymentRequest.TenantShare> shares,
                                                  Tenant initiatedBy) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new EntityNotFoundException("Bill not found: " + billId));

        Payment parentPayment = new Payment();
        parentPayment.setBill(bill);
        parentPayment.setTenant(initiatedBy);
        parentPayment.setAmount(bill.getTotalAmount());
        parentPayment.setStatus(Payment.Status.INITIATED);
        Payment savedParent = paymentRepository.save(parentPayment);

        return shares.stream().map(share -> {
            Tenant tenant = tenantRepository.findById(share.tenantId())
                    .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + share.tenantId()));

            PaymentSplit split = new PaymentSplit();
            split.setPayment(savedParent);
            split.setTenant(tenant);
            split.setShareAmount(share.amount());
            split.setStatus(PaymentSplit.Status.PENDING);
            // A real UPI link per tenant would be generated via Razorpay's Payment
            // Links API here — left as a follow-up since it needs a decision on
            // whether each tenant gets their own Razorpay order or a shared UPI
            // collect link; both are valid, this just hasn't been decided yet.
            return paymentSplitRepository.save(split);
        }).toList();
    }

    @Transactional
    public Payment verifyAndRecordPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new EntityNotFoundException("No payment found for order: " + razorpayOrderId));

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);

            boolean valid = Utils.verifyPaymentSignature(options, keySecret);
            if (!valid) {
                payment.setStatus(Payment.Status.FAILED);
                paymentRepository.save(payment);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Payment signature verification failed");
            }

            return recordSuccess(payment, razorpayPaymentId, "checkout-verify");
        } catch (RazorpayException e) {
            log.error("Signature verification threw", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not verify payment");
        }
    }

    /**
     * Called only from RazorpayWebhookController, which has ALREADY verified
     * the webhook's own signature (a different scheme than the checkout
     * signature above — HMAC over the raw payload with the webhook secret,
     * not order_id|payment_id with the key secret). Re-running
     * verifyPaymentSignature here would be wrong since the signature in a
     * webhook call isn't in that format at all. Authenticity for this path
     * comes from the caller having already verified the webhook signature.
     */
    @Transactional
    public Payment recordPaymentFromVerifiedWebhook(String razorpayOrderId, String razorpayPaymentId) {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new EntityNotFoundException("No payment found for order: " + razorpayOrderId));
        return recordSuccess(payment, razorpayPaymentId, "razorpay-webhook");
    }

    private Payment recordSuccess(Payment payment, String razorpayPaymentId, String source) {
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setStatus(Payment.Status.SUCCESS);
        payment.setPaidAt(java.time.LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

        billService.markPaid(payment.getBill().getId(), payment.getAmount(), source);

        auditService.log("Payment", saved.getId(), "VERIFIED_SUCCESS",
                null, razorpayPaymentId, source);
        return saved;
    }

    public List<Payment> findByTenantId(Long tenantId) {
        return paymentRepository.findByTenantId(tenantId);
    }
}
