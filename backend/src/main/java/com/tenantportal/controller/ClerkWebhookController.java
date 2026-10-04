package com.tenantportal.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.svix.Webhook;
import com.svix.exceptions.EmptyWebhookSecretException;
import com.svix.exceptions.WebhookVerificationException;
import com.tenantportal.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Receives Clerk's user.created (and user.updated) webhook events and syncs
 * them into the Tenant table via TenantService.syncFromClerk.
 *
 * Signature verification now real: Clerk delivers webhooks via Svix, which
 * signs every payload with HMAC-SHA256 over "{svix-id}.{svix-timestamp}.
 * {raw body}" using your endpoint's signing secret. Webhook.verify() below
 * recomputes that and rejects anything that doesn't match BEFORE the payload
 * is parsed or touches the database — this is what stops someone who finds
 * this URL from POSTing a fake payload and creating arbitrary Tenant rows,
 * which was the real gap this replaces.
 *
 * The @RequestBody is read as a raw String, not a parsed Map, deliberately —
 * signature verification needs the EXACT bytes Clerk signed. Re-serializing
 * through Jackson first (as the old version did) could reorder keys or
 * change whitespace and make a legitimate signature fail to verify. JSON
 * parsing happens only after verification succeeds.
 *
 * Needs clerk.webhook-secret set (starts with whsec_) — get this from
 * Clerk dashboard -> Webhooks -> your endpoint -> Signing Secret, after
 * you've registered this endpoint's URL there. Put it in Doppler, not
 * committed directly, same as the other secrets.
 */
@RestController
@RequestMapping("/api/webhooks/clerk")
@RequiredArgsConstructor
@Slf4j
public class ClerkWebhookController {

    private final TenantService tenantService;
    private final ObjectMapper objectMapper;

    @Value("${clerk.webhook-secret:}")
    private String webhookSecret;

    @PostMapping
    public void handleClerkWebhook(@RequestBody String rawBody, HttpServletRequest request) {
        JsonNode payload = verifyAndParse(rawBody, request);

        String type = payload.path("type").asText(null);
        if (!"user.created".equals(type) && !"user.updated".equals(type)) {
            return; // ignore events we don't care about
        }

        JsonNode data = payload.path("data");
        String clerkUserId = data.path("id").asText(null);

        JsonNode emailAddresses = data.path("email_addresses");
        String email = emailAddresses.isArray() && emailAddresses.size() > 0
                ? emailAddresses.get(0).path("email_address").asText(null)
                : null;

        String firstName = data.path("first_name").asText("");
        String lastName = data.path("last_name").asText("");
        String name = (firstName + " " + lastName).trim();
        String phone = null; // Clerk's phone_numbers array, if enabled — wire up when needed

        tenantService.syncFromClerk(clerkUserId, name, email, phone);
    }

    private JsonNode verifyAndParse(String rawBody, HttpServletRequest request) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            // Fail closed, not open — an unconfigured secret must never be
            // treated as "skip verification."
            log.error("clerk.webhook-secret is not set — rejecting webhook rather than processing unverified");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Webhook verification not configured");
        }

        String svixId = request.getHeader("svix-id");
        String svixTimestamp = request.getHeader("svix-timestamp");
        String svixSignature = request.getHeader("svix-signature");
        if (svixId == null || svixTimestamp == null || svixSignature == null) {
            // List.of() below throws on a null element rather than returning
            // a clean error — reject explicitly first so a malformed/missing
            // header is a 401, not an uncaught NullPointerException (500).
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing svix headers");
        }

        // This version of the svix library wants Map<String, List<String>>,
        // not Map<String, String> — confirmed against the actual compiler
        // error rather than the (older-API) docs. Each header has exactly
        // one value here, just wrapped in a singleton list to match.
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("svix-id", List.of(svixId));
        headers.put("svix-timestamp", List.of(svixTimestamp));
        headers.put("svix-signature", List.of(svixSignature));

        try {
            // Webhook's constructor also throws (EmptyWebhookSecretException)
            // if the secret is blank — belt-and-suspenders with the explicit
            // blank check above, which already rejects before reaching here,
            // but the compiler doesn't know that, so it's still a checked
            // exception this try block needs to handle.
            Webhook webhook = new Webhook(webhookSecret);
            webhook.verify(rawBody, headers);
        } catch (WebhookVerificationException | EmptyWebhookSecretException e) {
            log.warn("Clerk webhook signature verification failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }

        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed webhook payload");
        }
    }
}