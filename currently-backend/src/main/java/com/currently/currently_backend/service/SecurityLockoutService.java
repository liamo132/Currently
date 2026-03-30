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

    public void assertLoginAllowed(String principalKey) {
        assertAllowed(loginAttempts, principalKey, "Too many failed login attempts. Try again later.");
    }

    public void recordLoginFailure(String principalKey) {
        recordFailure(loginAttempts, principalKey, maxLoginFailures, loginLockoutMillis);
    }

    public void recordLoginSuccess(String principalKey) {
        loginAttempts.remove(principalKey);
    }

    public void assertVaultAllowed(Long userId) {
        assertAllowed(vaultAttempts, "vault:" + userId, "Too many failed vault PIN attempts. Try again later.");
    }

    public void recordVaultFailure(Long userId) {
        recordFailure(vaultAttempts, "vault:" + userId, maxVaultFailures, vaultLockoutMillis);
    }

    public void recordVaultSuccess(Long userId) {
        vaultAttempts.remove("vault:" + userId);
    }

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

    private static final class AttemptState {
        private int failures;
        private long lockedUntilEpochMillis;
    }
}
