package com.currently.currently_backend.service;

import com.currently.currently_backend.dto.BillFileResponse;
import com.currently.currently_backend.model.BillFile;
import com.currently.currently_backend.model.User;
import com.currently.currently_backend.repository.BillFileRepository;
import com.currently.currently_backend.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class BillsVaultService {

    private final UserRepository userRepository;
    private final BillFileRepository billFileRepository;
    private final PasswordEncoder passwordEncoder;

    public BillsVaultService(UserRepository userRepository,
                             BillFileRepository billFileRepository,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.billFileRepository = billFileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Helper: current user from JWT (your JWT sets auth name to email)
    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof User) {
            return (User) principal;  // ✅ Direct access, no DB query
        }

        // Fallback (shouldn't happen with fix above)
        String email = principal.toString();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found for email: " + email));
    }

    // Student rule: PIN must be exactly 4 digits.
    private void validatePinFormat(String pin) {
        if (pin == null || !pin.matches("^\\d{4}$")) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits.");
        }
    }

    public boolean hasPinSet() {
        User user = getCurrentUser();
        return user.getVaultPinHash() != null && !user.getVaultPinHash().isBlank();
    }

    // Set PIN only if not set already (first time setup)
    public void setPinFirstTime(String pin) {
        validatePinFormat(pin);

        User user = getCurrentUser();
        if (hasPinSet()) {
            throw new IllegalStateException("Vault PIN is already set.");
        }

        user.setVaultPinHash(passwordEncoder.encode(pin));
        userRepository.save(user);
    }

    // Verify PIN for any vault action
    public void verifyPinOrThrow(String pin) {
        validatePinFormat(pin);

        User user = getCurrentUser();
        if (!hasPinSet()) {
            throw new IllegalStateException("Vault PIN not set yet.");
        }

        if (!passwordEncoder.matches(pin, user.getVaultPinHash())) {
            throw new IllegalArgumentException("Invalid PIN.");
        }
    }

    public BillFileResponse uploadPdf(String pin, MultipartFile file) {
        verifyPinOrThrow(pin);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required.");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType();
        String filename = file.getOriginalFilename() == null ? "bill.pdf" : file.getOriginalFilename();

        // “Student security”: enforce PDFs only
        if (!contentType.equalsIgnoreCase("application/pdf") && !filename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed.");
        }

        try {
            byte[] bytes = file.getBytes();
            String sha256 = sha256Hex(bytes);

            User user = getCurrentUser();

            BillFile bf = new BillFile();
            bf.setUser(user);
            bf.setOriginalFilename(filename);
            bf.setContentType("application/pdf");
            bf.setFileSize((long) bytes.length);
            bf.setSha256(sha256);
            bf.setUploadedAt(LocalDateTime.now());
            bf.setData(bytes);

            BillFile saved = billFileRepository.save(bf);

            return new BillFileResponse(
                    saved.getId(),
                    saved.getOriginalFilename(),
                    saved.getContentType(),
                    saved.getFileSize(),
                    saved.getSha256(),
                    saved.getUploadedAt()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload PDF: " + e.getMessage());
        }
    }

    public List<BillFileResponse> listFiles(String pin) {
        verifyPinOrThrow(pin);
        User user = getCurrentUser();

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

    public BillFile getFileForDownload(String pin, Long fileId) {
        verifyPinOrThrow(pin);
        User user = getCurrentUser();

        return billFileRepository.findByIdAndUserId(fileId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("File not found."));
    }

    public void deleteFile(String pin, Long fileId) {
        verifyPinOrThrow(pin);
        User user = getCurrentUser();

        BillFile bf = billFileRepository.findByIdAndUserId(fileId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("File not found."));

        billFileRepository.delete(bf);
    }

    private String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(data);
        return HexFormat.of().formatHex(digest);
    }
}
