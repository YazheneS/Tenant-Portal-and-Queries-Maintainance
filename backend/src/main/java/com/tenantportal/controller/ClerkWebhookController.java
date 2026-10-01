package com.tenantportal.controller;

import com.tenantportal.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Receives Clerk's user.created (and user.updated) webhook events and syncs
 * them into the Tenant table via TenantService.syncFromClerk.
 *
 * IMPORTANT — not yet production-safe: this does not verify Clerk's webhook
 * signature (sent via the svix-id / svix-timestamp / svix-signature headers).
 * Without that check, anyone who finds this URL could POST a fake payload
 * and create arbitrary Tenant rows. Before deploying, add the `svix` library
 * (Clerk's webhook docs show the exact snippet) and verify the signature
 * before touching the database. Left as a TODO rather than guessed at, since
 * it needs your actual Clerk webhook signing secret to test against.
 */
@RestController
@RequestMapping("/api/webhooks/clerk")
@RequiredArgsConstructor
public class ClerkWebhookController {

    private final TenantService tenantService;

    @SuppressWarnings("unchecked")
    @PostMapping
    public void handleClerkWebhook(@RequestBody Map<String, Object> payload) {
        String type = (String) payload.get("type");
        if (!"user.created".equals(type) && !"user.updated".equals(type)) {
            return; // ignore events we don't care about
        }

        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        String clerkUserId = (String) data.get("id");

        var emailAddresses = (java.util.List<Map<String, Object>>) data.get("email_addresses");
        String email = emailAddresses != null && !emailAddresses.isEmpty()
                ? (String) emailAddresses.get(0).get("email_address")
                : null;

        String firstName = (String) data.getOrDefault("first_name", "");
        String lastName = (String) data.getOrDefault("last_name", "");
        String name = (firstName + " " + lastName).trim();
        String phone = null; // Clerk's phone_numbers array, if enabled — wire up when needed

        tenantService.syncFromClerk(clerkUserId, name, email, phone);
    }
}
