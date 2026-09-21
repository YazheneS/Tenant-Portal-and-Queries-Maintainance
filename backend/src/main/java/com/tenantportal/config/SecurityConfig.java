package com.tenantportal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;

/**
 * Phase 2 — Module 2 slice 1: Clerk-backed auth for the Owner/Admin role.
 * Vendor routes are intentionally left `permitAll()` here — they're gated by a
 * one-time-use UUID token (Spring Security OTT) checked in a separate filter/
 * controller, not by JWT. Tenant Passkey (WebAuthn) auth will register its own
 * chain / filter added alongside this one in the next slice.
 * Clerk issues a JWT per session. We verify it as a standard OAuth2 resource
 * server would: fetch Clerk's JWKS, validate signature + issuer + expiry.
 * Role (OWNER) is expected as a custom claim on the JWT template you configure
 * in the Clerk dashboard (Sessions → Edit JWT template), e.g.:
 *   { "role": "{{user.public_metadata.role}}" }
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${clerk.issuer}")
    private String clerkIssuer;

    @Value("${clerk.jwks-uri}")
    private String clerkJwksUri;

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Vendor: token-based, no login — checked inside the controller/filter, not here.
                        .requestMatchers("/api/vendor/**").permitAll()
                        // Public bootstrap endpoints (health check, webhook receivers, etc.)
                        .requestMatchers("/actuator/health", "/api/webhooks/**").permitAll()
                        // Admin/owner-only surface
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_OWNER")
                        // Tenant surface — will be widened to accept Passkey-issued auth too
                        .requestMatchers("/api/tenant/**").hasAnyAuthority("ROLE_OWNER", "ROLE_TENANT")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(clerkJwtDecoder())
                                .jwtAuthenticationConverter(clerkAuthenticationConverter())
                        )
                );
        return http.build();
    }

    @Bean
    public JwtDecoder clerkJwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(clerkJwksUri).build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(clerkIssuer);
        OAuth2TokenValidator<Jwt> withAudience = jwt -> {
            // Optional: tighten further if you set an `azp`/audience claim in the
            // Clerk JWT template to scope tokens to this API specifically.
            return OAuth2TokenValidatorResult.success();
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
        return decoder;
    }

    private JwtAuthenticationConverter clerkAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role"); // "OWNER" or "TENANT" from Clerk public_metadata
            Collection<GrantedAuthority> authorities = defaultConverter.convert(jwt);
            if (role != null && !role.isBlank()) {
                authorities = new java.util.ArrayList<>(authorities);
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            }
            return authorities;
        });
        // Clerk's `sub` claim is the Clerk user id — matches Tenant.clerkUserId
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}