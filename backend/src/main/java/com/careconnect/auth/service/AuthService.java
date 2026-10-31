package com.careconnect.auth.service;

import com.careconnect.audit.model.AuditAction;
import com.careconnect.audit.service.AuditService;
import com.careconnect.auth.dto.AuthDtos.ChangePasswordRequest;
import com.careconnect.auth.dto.AuthDtos.LoginRequest;
import com.careconnect.auth.dto.AuthDtos.TokenResponse;
import com.careconnect.auth.dto.AuthDtos.UserInfo;
import com.careconnect.auth.model.RefreshToken;
import com.careconnect.auth.repository.RefreshTokenRepository;
import com.careconnect.common.exception.ApiException;
import com.careconnect.config.AppProperties;
import com.careconnect.security.JwtService;
import com.careconnect.security.SecurityUtils;
import com.careconnect.security.UserPrincipal;
import com.careconnect.user.model.AppUser;
import com.careconnect.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Login, token refresh (with rotation and reuse detection), logout and password change.
 *
 * Refresh-token rotation: every refresh returns a NEW refresh token and revokes the old one.
 * If a revoked token is presented again, it was probably stolen, so all of that user's
 * sessions are revoked and they must log in again.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Hash of a random password; used so unknown usernames take as long to reject as wrong passwords. */
    private static final String DUMMY_HASH = "$2a$10$jOOAVYspAXHK.zBuFeBkWezfBqHODeShEhLFmHvRqc35cYaALK3/a";

    private final AppUserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final AppProperties properties;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String username = request.username().trim();
        AppUser user = users.findByUsernameIgnoreCase(username).orElse(null);
        boolean passwordOk = passwordEncoder.matches(request.password(),
                user == null ? DUMMY_HASH : user.getPasswordHash());

        if (user == null || !passwordOk || !user.isActive()) {
            String reason = user == null ? "unknown user" : !passwordOk ? "wrong password" : "account disabled";
            auditService.recordAuthEvent(AuditAction.LOGIN_FAILED, user == null ? null : user.getId(), username,
                    user == null ? null : user.getRole().name(), "Login failed: " + reason);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username or password.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        auditService.recordAuthEvent(AuditAction.LOGIN, user.getId(), user.getUsername(), user.getRole().name(), null);
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokens.findByTokenHash(sha256(rawRefreshToken))
                .orElseThrow(AuthService::invalidRefreshToken);
        AppUser user = stored.getUser();

        if (stored.isRevoked()) {
            // Reuse of an already-rotated token: treat as theft and end every session of this user.
            refreshTokens.revokeAllForUser(user.getId());
            throw invalidRefreshToken();
        }
        if (stored.getExpiresAt().isBefore(LocalDateTime.now()) || !user.isActive()) {
            stored.setRevoked(true);
            throw invalidRefreshToken();
        }
        stored.setRevoked(true);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.findByTokenHash(sha256(rawRefreshToken)).ifPresent(token -> {
            token.setRevoked(true);
            AppUser user = token.getUser();
            auditService.recordAuthEvent(AuditAction.LOGOUT, user.getId(), user.getUsername(), user.getRole().name(), null);
        });
    }

    @Transactional(readOnly = true)
    public UserInfo me() {
        AppUser user = users.findById(SecurityUtils.currentUser().id())
                .orElseThrow(() -> ApiException.notFound("User", SecurityUtils.currentUser().id()));
        return toUserInfo(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        AppUser user = users.findById(SecurityUtils.currentUser().id())
                .orElseThrow(() -> ApiException.notFound("User", SecurityUtils.currentUser().id()));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Current password is incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokens.revokeAllForUser(user.getId());
    }

    /** Housekeeping: remove long-expired refresh tokens every night at 02:00. */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        refreshTokens.deleteExpired(LocalDateTime.now().minusDays(1));
    }

    private TokenResponse issueTokens(AppUser user) {
        String rawRefresh = newRandomToken();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(sha256(rawRefresh));
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusDays(properties.jwt().refreshTokenDays()));
        refreshTokens.save(token);

        String access = jwtService.generateAccessToken(UserPrincipal.from(user));
        return new TokenResponse(access, rawRefresh, "Bearer", jwtService.accessTokenTtlSeconds(), toUserInfo(user));
    }

    public static UserInfo toUserInfo(AppUser user) {
        return new UserInfo(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(), user.getRole(),
                user.getPatient() == null ? null : user.getPatient().getId(),
                user.getDepartment() == null ? null : user.getDepartment().getName(),
                user.getSpecialty());
    }

    private String newRandomToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Your session has ended. Please sign in again.");
    }
}
