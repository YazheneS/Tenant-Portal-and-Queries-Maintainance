package com.tenantportal.controller;

import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.tenantportal.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Receives Razorpay's payment.captured webhook as a backup confirmation path
 * (the primary path is PaymentController.verify(), called directly by the
 * frontend after Checkout succeeds). Unlike the Clerk webhook, this one DOES
 * verify its signature — Razorpay's SDK provides Utils.verifyWebhookSignature
 * directly, so there's no reason to leave it unverified.
 *
 * Needs razorpay.webhook-secret set — generate this in the Razorpay dashboard
 * under Webhooks when you register this endpoint's URL.
 */
@RestController
@RequestMapping("/api/webhooks/razorpay")
@RequiredArgsConstructor
@Slf4j
public class RazorpayWebhookController {

    private final PaymentService paymentService;

    @Value("${razorpay.webhook-secret:}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody String rawPayload,
                                               @RequestHeader("X-Razorpay-Signature") String signature) {
        try {
            boolean valid = Utils.verifyWebhookSignature(rawPayload, signature, webhookSecret);
            if (!valid) {
                log.warn("Razorpay webhook signature invalid — rejecting");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            JSONObject payload = new JSONObject(rawPayload);
            String event = payload.optString("event");

            if ("payment.captured".equals(event)) {
                JSONObject paymentEntity = payload.getJSONObject("payload")
                        .getJSONObject("payment").getJSONObject("entity");
                String orderId = paymentEntity.getString("order_id");
                String paymentId = paymentEntity.getString("id");
                // Signature already verified above (webhook scheme, not the
                // checkout scheme) — go straight to recording, skip re-verification.
                paymentService.recordPaymentFromVerifiedWebhook(orderId, paymentId);
            }

            return ResponseEntity.ok().build();
        } catch (RazorpayException e) {
            log.error("Webhook signature verification threw", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
