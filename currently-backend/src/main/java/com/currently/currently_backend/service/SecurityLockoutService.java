package com.currently.currently_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SecurityLockoutService {

    private final Map<String, AttemptState> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, AttemptState> vaultAttempts = new ConcurrentHashMap<>();
    private final int maxLoginFailures;
    private final long loginLockoutMillis;
    private final int maxVaultFailures;
    private final long vaultLockoutMillis;

    public SecurityLockoutService(
            @Value("${security.auth.max-failures:5}") int maxLoginFailures,
            @Value("${security.auth.lockout-minutes:15}") int loginLockoutMinutes,
            @Value("${security.vault.max-pin-failures:5}") int maxVaultFailures,
            @Value("${security.vault.lockout-minutes:15}") int vaultLockoutMinutes
    ) {
        this.maxLoginFailures = maxLoginFailures;
        this.loginLockoutMillis = Duration.ofMinutes(loginLockoutMinutes).toMillis();
        this.maxVaultFailures = maxVaultFailures;
        this.vaultLockoutMillis = Duration.ofMinutes(vaultLockoutMinutes).toMillis();
    }

    // Security Lockout: blocks Login when the same principal has too many recent failed attempts.
    public void assertLoginAllowed(String principalKey) {
        assertAllowed(loginAttempts, principalKey, "Too many failed login attempts. Try again later.");
    }

    // Security Lockout: records one failed Login and may start the configured lockout window.
    public void recordLoginFailure(String principalKey) {
        recordFailure(loginAttempts, principalKey, maxLoginFailures, loginLockoutMillis);
    }

    // Security Lockout: clears failed Login attempts after a successful authentication.
    public void recordLoginSuccess(String principalKey) {
        loginAttempts.remove(principalKey);
    }

    // Security Lockout: blocks Bills Vault actions when a user has too many failed PIN attempts.
    public void assertVaultAllowed(Long userId) {
        assertAllowed(vaultAttempts, "vault:" + userId, "Too many failed vault PIN attempts. Try again later.");
    }

    // Security Lockout: records one failed Bills Vault PIN attempt.
    public void recordVaultFailure(Long userId) {
        recordFailure(vaultAttempts, "vault:" + userId, maxVaultFailures, vaultLockoutMillis);
    }

    // Security Lockout: clears failed Bills Vault PIN attempts after a correct PIN.
    public void recordVaultSuccess(Long userId) {
        vaultAttempts.remove("vault:" + userId);
    }

    // Security helper: checks whether a key is currently locked and removes expired lockouts.
    private void assertAllowed(Map<String, AttemptState> attempts, String key, String message) {
        AttemptState state = attempts.get(key);
        if (state == null) {
            return;
        }

        long now = System.currentTimeMillis();
        synchronized (state) {
            if (state.lockedUntilEpochMillis > now) {
                throw new IllegalStateException(message);
            }
            if (state.lockedUntilEpochMillis != 0L && state.lockedUntilEpochMillis <= now) {
                attempts.remove(key);
            }
        }
    }

    // Security helper: increments failure counts and sets lockedUntil when the limit is reached.
    private void recordFailure(Map<String, AttemptState> attempts, String key, int maxFailures, long lockoutMillis) {
        long now = System.currentTimeMillis();
        AttemptState state = attempts.computeIfAbsent(key, ignored -> new AttemptState());
        synchronized (state) {
            if (state.lockedUntilEpochMillis > now) {
                return;
            }

            state.failures++;
            if (state.failures >= maxFailures) {
                state.lockedUntilEpochMillis = now + lockoutMillis;
            }
        }
    }

    // Security state: tracks failed attempts and the timestamp when a lockout expires.
    private static final class AttemptState {
        private int failures;
        private long lockedUntilEpochMillis;
    }
}
