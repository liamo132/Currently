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
 * File: BillsVaultController.java
 * Description: Handles the Bills Vault API for PIN-protected PDF upload, listing, download, and delete actions.
 * Project: Currently
 * Author: Liam Connell
 *
 */
@RestController
@RequestMapping("/api/vault")
public class BillsVaultController {

    private final BillsVaultService vaultService;

    public BillsVaultController(BillsVaultService vaultService) {
        this.vaultService = vaultService;
    }

    // Controller API: tells the frontend whether this authenticated user has already created a Vault PIN.
    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of("pinSet", vaultService.hasPinSet()));
    }

    // Controller API: sets the first 4-digit Bills Vault PIN after backend Validation.
    @PostMapping("/pin")
    public ResponseEntity<?> setPin(@Valid @RequestBody VaultPinRequest req) {
        vaultService.setPinFirstTime(req.getPin());
        return ResponseEntity.ok(Map.of("message", "PIN set successfully."));
    }

    // Controller API: unlocks the Bills Vault UI by verifying the submitted PIN against the stored hash.
    @PostMapping("/unlock")
    public ResponseEntity<?> unlock(@Valid @RequestBody VaultPinRequest req) {
        vaultService.verifyPinOrThrow(req.getPin());
        return ResponseEntity.ok(Map.of("unlocked", true));
    }

    /*
     * Controller API: Bills Vault upload
     * Purpose: Receives multipart PDF data plus the user's PIN, then delegates file type checks,
     * Encryption, Database storage, and Audit Logging to BillsVaultService.
     */
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BillFileResponse> upload(
            @RequestParam("pin") String pin,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(vaultService.uploadPdf(pin, file));
    }

    // Controller API: lists the authenticated user's saved bill metadata after PIN verification.
    @PostMapping("/files/list")
    public ResponseEntity<List<BillFileResponse>> list(@Valid @RequestBody VaultPinRequest req) {
        return ResponseEntity.ok(vaultService.listFiles(req.getPin()));
    }

    /*
     * Controller API: Bills Vault download
     * Purpose: Returns decrypted PDF bytes with no-store cache headers so sensitive bills are not cached by browsers.
     */
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

    // Controller API: deletes one bill owned by the authenticated user after PIN verification.
    @DeleteMapping("/files/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, @RequestParam("pin") String pin) {
        vaultService.deleteFile(pin, id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }
}
