package com.tenantportal.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limits /api/vendor/** by client IP — this is the piece from the
 * original auth design note ("DPoP + Bucket4J on payment endpoints") applied
 * to the vendor surface instead, since that's the other publicly-reachable,
 * token-guessable endpoint in the system (payments already get their
 * protection from Razorpay's own signature verification, so Bucket4J matters
 * more here where the "secret" is just a UUID in a URL).
 *
 * In-memory bucket per IP — fine for a single instance. If this ever runs
 * behind multiple backend instances, this needs to move to a shared store
 * (Redis) instead, since each instance would otherwise track its own
 * independent limit.
 *
 * NOT a @Component — registered explicitly as a bean in SecurityConfig and
 * added into the security filter chain there, rather than letting Spring
 * Boot auto-register it as a servlet filter applying to every request.
 */
public class VendorRateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket newBucket() {
        // 30 requests per minute per IP — generous for a real vendor
        // refreshing their job page, tight enough to blunt UUID brute-forcing.
        Bandwidth limit = Bandwidth.classic(30, io.github.bucket4j.Refill.intervally(30, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/vendor/")) {
            String clientIp = request.getRemoteAddr();
            Bucket bucket = buckets.computeIfAbsent(clientIp, ip -> newBucket());

            if (!bucket.tryConsume(1)) {
                response.setStatus(429);
                response.getWriter().write("Too many requests — try again shortly.");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
