package com.currently.currently_backend.service;

import com.currently.currently_backend.util.DataProtectionUtil;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class UserLookupHashService {

    // Service: creates a deterministic HMAC hash for email lookup while keeping the email itself encrypted.
    public String emailHash(String email) {
        return DataProtectionUtil.hmacSha256(normalize(email));
    }

    // Service: creates a deterministic HMAC hash for username uniqueness checks during Register.
    public String usernameHash(String username) {
        return DataProtectionUtil.hmacSha256(normalize(username));
    }

    // Validation helper: normalizes user identifiers before hashing so Login/Register comparisons are consistent.
    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
