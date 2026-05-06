/*
 * File: JwtUtil.java
 * Description: Utility class for generating and validating JWT tokens.
 * Author: Liam Connell
 * Date: 2025-11-11
 */

package com.currently.currently_backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final Key signingKey;
    private final long expirationMs;

    public JwtUtil(
            Environment environment,
            @Value("${app.jwt.secret:}") String configuredSecret,
            @Value("${app.jwt.expiration-ms:3600000}") long expirationMs
    ) {
        this.signingKey = buildSigningKey(environment, configuredSecret);
        this.expirationMs = expirationMs;
    }

    // JWT helper: creates a signed token with the user's email as the subject for authenticated API calls.
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // JWT helper: extracts the subject from the token, which Currently treats as the Login email.
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // JWT helper: extracts any single claim using a caller-provided resolver.
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // JWT helper: validates that the token belongs to the expected user and has not expired.
    public boolean validateToken(String token, String username) {
        return username.equals(extractUsername(token)) && !isTokenExpired(token);
    }

    // JWT helper: checks expiration time against the current server clock.
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // JWT helper: reads the expiration claim used by Authentication validation.
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // JWT helper: verifies the signature and returns all claims from the token body.
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /*
     * Security helper: JWT signing key
     * Purpose: Uses configured Base64 JWT_SECRET in production, but allows an ephemeral dev key outside prod.
     */
    private Key buildSigningKey(Environment environment, String configuredSecret) {
        if (configuredSecret == null || configuredSecret.trim().isEmpty()) {
            if (environment.acceptsProfiles(Profiles.of("prod"))) {
                throw new IllegalStateException("JWT_SECRET must be set when running with the prod profile.");
            }
            log.warn("JWT_SECRET is not set. Using an ephemeral signing key for this run only.");
            return Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }

        try {
            byte[] keyBytes = Decoders.BASE64.decode(configuredSecret.trim());
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Invalid JWT secret. Set JWT_SECRET as Base64 for a 32+ byte key.",
                    ex
            );
        }
    }
}
