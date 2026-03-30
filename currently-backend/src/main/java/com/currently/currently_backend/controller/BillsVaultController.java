package com.currently.currently_backend.controller;

import com.currently.currently_backend.dto.BillFileResponse;
import com.currently.currently_backend.dto.VaultPinRequest;
import com.currently.currently_backend.model.BillFile;
import com.currently.currently_backend.service.BillsVaultService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/*
 * Bills Vault API:
 * - First time: user sets a 4-digit PIN
 * - Every action requires PIN (simple “second factor” on top of JWT)
 */
@RestController
@RequestMapping("/api/vault")
public class BillsVaultController {

    private final BillsVaultService vaultService;

    public BillsVaultController(BillsVaultService vaultService) {
        this.vaultService = vaultService;
    }

    // Check if a PIN exists (frontend decides whether to show "create pin" UI)
    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of("pinSet", vaultService.hasPinSet()));
    }

    // Set PIN (first time only)
    @PostMapping("/pin")
    public ResponseEntity<?> setPin(@Valid @RequestBody VaultPinRequest req) {
        vaultService.setPinFirstTime(req.getPin());
        return ResponseEntity.ok(Map.of("message", "PIN set successfully."));
    }

    // Verify PIN (optional endpoint for "unlock" button)
    @PostMapping("/unlock")
    public ResponseEntity<?> unlock(@Valid @RequestBody VaultPinRequest req) {
        vaultService.verifyPinOrThrow(req.getPin());
        return ResponseEntity.ok(Map.of("unlocked", true));
    }

    // Upload PDF: multipart + pin
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BillFileResponse> upload(
            @RequestParam("pin") String pin,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(vaultService.uploadPdf(pin, file));
    }

    // List files
    @PostMapping("/files/list")
    public ResponseEntity<List<BillFileResponse>> list(@Valid @RequestBody VaultPinRequest req) {
        return ResponseEntity.ok(vaultService.listFiles(req.getPin()));
    }

    // Download file
    @PostMapping("/files/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id, @Valid @RequestBody VaultPinRequest req) {
        BillFile bf = vaultService.getFileForDownload(req.getPin(), id);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(bf.getOriginalFilename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(bf.getData());
    }

    // Delete file
    @DeleteMapping("/files/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, @RequestParam("pin") String pin) {
        vaultService.deleteFile(pin, id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }
}
