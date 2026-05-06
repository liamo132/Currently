package com.currently.currently_backend.config;

import com.currently.currently_backend.util.DataProtectionUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DataProtectionBootstrap {

    @Value("${app.data.encryption-key:}")
    private String encryptionKey;

    @Value("${app.data.hash-key:}")
    private String hashKey;

    // Encryption bootstrap: configures AES-GCM and HMAC keys before encrypted fields are read or written.
    @PostConstruct
    public void initialize() {
        DataProtectionUtil.configure(encryptionKey, hashKey);
    }
}
