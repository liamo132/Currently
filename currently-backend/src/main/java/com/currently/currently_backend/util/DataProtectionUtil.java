package com.currently.currently_backend.util;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class DataProtectionUtil {

    private static final String ENC_ALGORITHM = "AES";
    private static final String ENC_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String ENCRYPTED_PREFIX = "enc::";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static volatile SecretKeySpec encryptionKeySpec;
    private static volatile SecretKeySpec hashKeySpec;

    private DataProtectionUtil() {
    }

    // Encryption setup: configures AES-GCM encryption and HMAC lookup keys from application properties/env vars.
    public static void configure(String encryptionKeyBase64, String hashKeyBase64) {
        encryptionKeySpec = toAesKey(encryptionKeyBase64);
        hashKeySpec = toHmacKey(hashKeyBase64 == null || hashKeyBase64.isBlank()
                ? encryptionKeyBase64
                : hashKeyBase64);
    }

    /*
     * Encryption helper: text fields
     * Purpose: Encrypts sensitive strings before Database storage using AES-GCM with a random IV per value.
     */
    public static String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        ensureConfigured();
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ENC_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKeySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(cipherText, 0, payload, iv.length, cipherText.length);

            return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt value", ex);
        }
    }

    /*
     * Encryption helper: text fields
     * Purpose: Decrypts AES-GCM protected strings after Database read; non-prefixed values are returned for
     * backward compatibility with older plaintext rows.
     */
    public static String decrypt(String encryptedValue) {
        if (encryptedValue == null) {
            return null;
        }
        if (!encryptedValue.startsWith(ENCRYPTED_PREFIX)) {
            return encryptedValue;
        }
        ensureConfigured();
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedValue.substring(ENCRYPTED_PREFIX.length()));
            if (payload.length < IV_LENGTH_BYTES + 1) {
                return encryptedValue;
            }

            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] cipherText = new byte[payload.length - IV_LENGTH_BYTES];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(payload, IV_LENGTH_BYTES, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ENC_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKeySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            // Backward compatibility for pre-encryption plaintext rows.
            return encryptedValue;
        }
    }

    // Security helper: creates deterministic HMAC-SHA256 hashes for searchable encrypted identifiers.
    public static String hmacSha256(String value) {
        if (value == null) {
            return null;
        }
        ensureConfigured();
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hashKeySpec);
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash value", ex);
        }
    }

    // Encryption helper: encrypts raw PDF bytes for Bills Vault Database storage.
    public static byte[] encryptBytes(byte[] plainBytes) {
        if (plainBytes == null) {
            return null;
        }
        ensureConfigured();
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ENC_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKeySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainBytes);

            byte[] payload = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(cipherText, 0, payload, iv.length, cipherText.length);
            return payload;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt bytes", ex);
        }
    }

    // Encryption helper: decrypts raw PDF bytes when the user downloads a Bills Vault file.
    public static byte[] decryptBytes(byte[] encryptedBytes) {
        if (encryptedBytes == null) {
            return null;
        }
        ensureConfigured();
        try {
            if (encryptedBytes.length < IV_LENGTH_BYTES + 1) {
                return Arrays.copyOf(encryptedBytes, encryptedBytes.length);
            }

            byte[] iv = Arrays.copyOfRange(encryptedBytes, 0, IV_LENGTH_BYTES);
            byte[] cipherText = Arrays.copyOfRange(encryptedBytes, IV_LENGTH_BYTES, encryptedBytes.length);

            Cipher cipher = Cipher.getInstance(ENC_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKeySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return cipher.doFinal(cipherText);
        } catch (Exception ex) {
            return Arrays.copyOf(encryptedBytes, encryptedBytes.length);
        }
    }

    // Encryption helper: lazily loads keys if DataProtectionBootstrap has not configured them yet.
    private static void ensureConfigured() {
        if (encryptionKeySpec != null && hashKeySpec != null) {
            return;
        }

        synchronized (DataProtectionUtil.class) {
            if (encryptionKeySpec != null && hashKeySpec != null) {
                return;
            }

            String encryptionKey = firstNonBlank(
                    System.getProperty("app.data.encryption-key"),
                    System.getenv("APP_DATA_ENCRYPTION_KEY")
            );
            String hashKey = firstNonBlank(
                    System.getProperty("app.data.hash-key"),
                    System.getenv("APP_DATA_HASH_KEY")
            );

            if (encryptionKey == null || encryptionKey.isBlank()) {
                throw new IllegalStateException("APP_DATA_ENCRYPTION_KEY is required for encrypted fields.");
            }

            configure(encryptionKey, hashKey);
        }
    }

    // Config helper: returns the first configured value from property or environment variable.
    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    // Validation helper: converts the Base64 AES key into a 32-byte SecretKeySpec for AES-256.
    private static SecretKeySpec toAesKey(String keyBase64) {
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        if (keyBytes.length != 32) {
            throw new IllegalStateException("APP_DATA_ENCRYPTION_KEY must be Base64 for 32 bytes.");
        }
        return new SecretKeySpec(keyBytes, ENC_ALGORITHM);
    }

    // Validation helper: converts the Base64 HMAC key into a SecretKeySpec for lookup hashing.
    private static SecretKeySpec toHmacKey(String keyBase64) {
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("APP_DATA_HASH_KEY must be Base64 for at least 32 bytes.");
        }
        return new SecretKeySpec(keyBytes, HMAC_ALGORITHM);
    }
}
