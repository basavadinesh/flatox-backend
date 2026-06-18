package com.flatox.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final String SECRET_KEY =
            "flatox_super_secret_key_flatox_super_secret_key";

    private final SecretKey key = Keys.hmacShaKeyFor(
            SECRET_KEY.getBytes(StandardCharsets.UTF_8)
    );

    public String generateToken(String phone) {
        return generateToken(phone, null);
    }

    public String generateToken(String phone, String approvalStatus) {
        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .setSubject(phone)
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(System.currentTimeMillis() + 86400000)
                );
        if (approvalStatus != null) {
            builder.claim("approvalStatus", approvalStatus);
        }
        return builder.signWith(key).compact();
    }

    public io.jsonwebtoken.Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractPhone(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractApprovalStatus(String token) {
        return extractAllClaims(token).get("approvalStatus", String.class);
    }

    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}