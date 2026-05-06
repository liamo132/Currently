package com.currently.currently_backend.dto;

import java.time.LocalDateTime;

/*
 * DTO: BillFileResponse
 * Purpose: Sends Bills Vault file metadata to React without exposing encrypted PDF bytes.
 */
public class BillFileResponse {
    private Long id;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private String sha256;
    private LocalDateTime uploadedAt;

    public BillFileResponse() {}

    public BillFileResponse(Long id, String originalFilename, String contentType, Long fileSize, String sha256, LocalDateTime uploadedAt) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.sha256 = sha256;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() { return id; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public Long getFileSize() { return fileSize; }
    public String getSha256() { return sha256; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
