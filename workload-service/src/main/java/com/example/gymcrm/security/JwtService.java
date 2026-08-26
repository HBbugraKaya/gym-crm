package com.example.gymcrm.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Service responsible for validating and parsing JSON Web Tokens (JWT)
 * used for inter-service authentication.
 */
@Service
public class JwtService {

    // Cryptographic secret key used to verify the HMAC-SHA256 signature
    private final SecretKey secretKey;

    /**
     * Constructs the JwtService and converts the secret string from application.yml
     * into a valid HMAC SecretKey object.
     *
     * @param secret 256-bit secret key string configured in application.yml
     */
    public JwtService(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates whether the given JWT token has a valid signature and has not expired.
     *
     * @param token incoming JWT token string
     * @return true if token is valid and unexpired; false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            // Verify that the expiration date is not before current timestamp
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            // Returns false if signature verification fails, token is malformed, or expired
            return false;
        }
    }

    /**
     * Extracts the subject (username) from the token payload.
     *
     * @param token JWT token string
     * @return username stored in the token subject claim
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Parses the signed JWT token using the secret key and extracts all claims.
     * Throws JwtException if signature is invalid or token is tampered with.
     *
     * @param token JWT token string
     * @return Claims payload containing token claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

