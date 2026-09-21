package com.tenantportal.config;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Small helper so controllers/services don't need to know JWT internals.
 * jwt.getSubject() is Clerk's `sub` claim — matches Tenant.clerkUserId,
 * which you already have on the entity.
 * Usage in a controller:
 *   String clerkUserId = currentUser.clerkUserId();
 *   Tenant tenant = tenantRepository.findByClerkUserId(clerkUserId)
 *       .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
 */
@Component
public class CurrentUser {

    public String clerkUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        throw new IllegalStateException("No authenticated Clerk JWT in context");
    }

    public String role() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt.getClaimAsString("role");
        }
        return null;
    }
}