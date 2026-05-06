package com.currently.currently_backend.service;

import com.currently.currently_backend.dto.BillFileResponse;
import com.currently.currently_backend.model.BillFile;
import com.currently.currently_backend.model.User;
import com.currently.currently_backend.repository.BillFileRepository;
import com.currently.currently_backend.repository.UserRepository;
import com.currently.currently_backend.util.DataProtectionUtil;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class BillsVaultService {

    private static final long MAX_PDF_BYTES = 5L * 1024L * 1024L;
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final UserRepository userRepository;
    private final BillFileRepository billFileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserLookupHashService userLookupHashService;
    private final SecurityLockoutService securityLockoutService;
    private final SecurityAuditService securityAuditService;

    public BillsVaultService(UserRepository userRepository,
                             BillFileRepository billFileRepository,
                             PasswordEncoder passwordEncoder,
                             UserLookupHashService userLookupHashService,
                             SecurityLockoutService securityLockoutService,
                             SecurityAuditService securityAuditService) {
        this.userRepository = userRepository;
        this.billFileRepository = billFileRepository;
        this.passwordEncoder = passwordEncoder;
        this.userLookupHashService = userLookupHashService;
        this.securityLockoutService = securityLockoutService;
        this.securityAuditService = securityAuditService;
    }

    /*
     * Service helper: resolves the authenticated user for Bills Vault actions.
     * Important logic: JWT filter may store the full User principal; otherwise this falls back to email hash lookup.
     */
    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof User user) {
            return user;
        }

        String email = principal.toString();
        return userRepository.findByEmailHash(userLookupHashService.emailHash(email))
                .orElseThrow(() -> new RuntimeException("User not found for email: " + email));
    }

    // Validation helper: enforces a simple 4-digit PIN format before hashing or checking Vault access.
    private void validatePinFormat(String pin) {
        if (pin == null || !pin.matches("^\\d{4}$")) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits.");
        }
    }

    // Service: returns whether the authenticated user has already created a Bills Vault PIN.
    public boolean hasPinSet() {
        User user = getCurrentUser();
        return user.getVaultPinHash() != null && !user.getVaultPinHash().isBlank();
    }

    /*
     * Service: Set first Vault PIN
     * Purpose: Validates the submitted PIN, prevents resetting an existing PIN through this endpoint,
     * hashes it with BCrypt, saves it on the User, and records Audit Logging.
     */
    public void setPinFirstTime(String pin) {
        validatePinFormat(pin);

        User user = getCurrentUser();
        if (hasPinSet()) {
            throw new IllegalStateException("Vault PIN is already set.");
        }

        user.setVaultPinHash(passwordEncoder.encode(pin));
        userRepository.save(user);
        securityAuditService.logVaultAction(user.getId(), "vault_pin_set", "status=created");
    }

    /*
     * Service: Verify Vault PIN
     * Purpose: Checks lockout state, confirms a PIN exists, compares the submitted PIN to the BCrypt hash,
     * records success/failure Audit Logging, and rejects invalid access.
     */
    public void verifyPinOrThrow(String pin) {
        validatePinFormat(pin);

        User user = getCurrentUser();
        securityLockoutService.assertVaultAllowed(user.getId());
        if (!hasPinSet()) {
            throw new IllegalStateException("Vault PIN not set yet.");
        }

        if (!passwordEncoder.matches(pin, user.getVaultPinHash())) {
            securityLockoutService.recordVaultFailure(user.getId());
            securityAuditService.logVaultPinFailure(user.getId(), "verify");
            throw new IllegalArgumentException("Invalid PIN.");
        }

        securityLockoutService.recordVaultSuccess(user.getId());
        securityAuditService.logVaultPinSuccess(user.getId(), "verify");
    }

    /*
     * Service: Upload PDF to Bills Vault
     * Purpose: Requires a valid PIN, validates file presence, size, content type, extension, and PDF signature,
     * encrypts PDF bytes, stores metadata and encrypted data in the Database, and logs the upload.
     */
    public BillFileResponse uploadPdf(String pin, MultipartFile file) {
        verifyPinOrThrow(pin);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required.");
        }
        if (file.getSize() > MAX_PDF_BYTES) {
            throw new IllegalArgumentException("PDF exceeds the maximum allowed size of 5 MB.");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType();
        String filename = sanitizeFilename(file.getOriginalFilename());
        if (!contentType.equalsIgnoreCase("application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed.");
        }
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed.");
        }

        try {
            byte[] bytes = file.getBytes();
            if (!hasPdfSignature(bytes)) {
                throw new IllegalArgumentException("Uploaded file content is not a valid PDF.");
            }

            String sha256 = sha256Hex(bytes);
            User user = getCurrentUser();

            BillFile billFile = new BillFile();
            billFile.setUser(user);
            billFile.setOriginalFilename(filename);
            billFile.setContentType("application/pdf");
            billFile.setFileSize((long) bytes.length);
            billFile.setSha256(sha256);
            billFile.setUploadedAt(LocalDateTime.now());
            billFile.setData(DataProtectionUtil.encryptBytes(bytes));

            BillFile saved = billFileRepository.save(billFile);
            securityAuditService.logVaultAction(
                    user.getId(),
                    "vault_file_upload",
                    "fileId=" + saved.getId() + " filename=" + saved.getOriginalFilename() + " size=" + saved.getFileSize()
            );

            return new BillFileResponse(
                    saved.getId(),
                    saved.getOriginalFilename(),
                    saved.getContentType(),
                    saved.getFileSize(),
                    saved.getSha256(),
                    saved.getUploadedAt()
            );
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload PDF: " + e.getMessage());
        }
    }

    // Service: lists this user's Bills Vault files after PIN verification without returning encrypted PDF bytes.
    public List<BillFileResponse> listFiles(String pin) {
        verifyPinOrThrow(pin);
        User user = getCurrentUser();
        securityAuditService.logVaultAction(user.getId(), "vault_file_list", "status=ok");

        return billFileRepository.findAllByUserIdOrderByUploadedAtDesc(user.getId())
                .stream()
                .map(b -> new BillFileResponse(
                        b.getId(),
                        b.getOriginalFilename(),
                        b.getContentType(),
                        b.getFileSize(),
                        b.getSha256(),
                        b.getUploadedAt()
                ))
                .toList();
    }

    /*
     * Service: Download Vault file
     * Purpose: Verifies the PIN, fetches the file by id and user id, decrypts the PDF bytes,
     * records Audit Logging, and returns the file entity to the Controller.
     */
    public BillFile getFileForDownload(String pin, Long fileId) {
        verifyPinOrThrow(pin);
        User user = getCurrentUser();

        BillFile billFile = billFileRepository.findByIdAndUserId(fileId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("File not found."));
        billFile.setData(DataProtectionUtil.decryptBytes(billFile.getData()));
        securityAuditService.logVaultAction(
                user.getId(),
                "vault_file_download",
                "fileId=" + billFile.getId() + " filename=" + billFile.getOriginalFilename()
        );
        return billFile;
    }

    // Service: deletes a Vault file only when the authenticated user owns the file and supplies the correct PIN.
    public void deleteFile(String pin, Long fileId) {
        verifyPinOrThrow(pin);
        User user = getCurrentUser();

        BillFile billFile = billFileRepository.findByIdAndUserId(fileId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("File not found."));

        billFileRepository.delete(billFile);
        securityAuditService.logVaultAction(
                user.getId(),
                "vault_file_delete",
                "fileId=" + fileId + " filename=" + billFile.getOriginalFilename()
        );
    }

    // Security helper: calculates a SHA-256 digest for file integrity metadata, not for encryption.
    private String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(data);
        return HexFormat.of().formatHex(digest);
    }

    // Upload Validation helper: confirms the PDF magic bytes "%PDF-" are present at the start of the upload.
    private boolean hasPdfSignature(byte[] bytes) {
        if (bytes == null || bytes.length < PDF_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < PDF_MAGIC.length; i++) {
            if (bytes[i] != PDF_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    // Security helper: strips path separators and line breaks from uploaded filenames before Database storage.
    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "bill.pdf";
        }

        String sanitized = originalFilename
                .replace("\\", "_")
                .replace("/", "_")
                .replace("\r", "_")
                .replace("\n", "_")
                .replace("\"", "_")
                .trim();

        return sanitized.isBlank() ? "bill.pdf" : sanitized;
    }
}
