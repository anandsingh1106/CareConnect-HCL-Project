package com.careconnect.security;

import com.careconnect.config.AppProperties;
import com.careconnect.user.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Creates and verifies short-lived access tokens (HMAC-SHA256 signed JWTs). */
@Service
public class JwtService {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_PATIENT_ID = "pid";
    private static final String CLAIM_NAME = "name";

    private final SecretKey key;
    private final Duration accessTokenTtl;

    public JwtService(AppProperties properties) {
        String secret = properties.jwt().secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("careconnect.jwt.secret (JWT_SECRET) must be at least 32 bytes long.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = Duration.ofMinutes(properties.jwt().accessTokenMinutes());
    }

    public String generateAccessToken(UserPrincipal user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.username())
                .claim(CLAIM_USER_ID, user.id())
                .claim(CLAIM_ROLE, user.role().name())
                .claim(CLAIM_PATIENT_ID, user.patientId())
                .claim(CLAIM_NAME, user.fullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key)
                .compact();
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }

    /** Returns the user described by a valid token; throws {@link JwtException} if it is invalid or expired. */
    public UserPrincipal parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Number patientId = claims.get(CLAIM_PATIENT_ID, Number.class);
        return new UserPrincipal(
                claims.get(CLAIM_USER_ID, Number.class).longValue(),
                claims.getSubject(),
                claims.get(CLAIM_NAME, String.class),
                Role.valueOf(claims.get(CLAIM_ROLE, String.class)),
                patientId == null ? null : patientId.longValue(),
                null,
                true);
    }
}
