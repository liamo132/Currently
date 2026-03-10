package com.currently.currently_backend.service;

import com.currently.currently_backend.util.DataProtectionUtil;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class UserLookupHashService {

    public String emailHash(String email) {
        return DataProtectionUtil.hmacSha256(normalize(email));
    }

    public String usernameHash(String username) {
        return DataProtectionUtil.hmacSha256(normalize(username));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
