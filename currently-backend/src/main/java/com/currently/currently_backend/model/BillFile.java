package com.currently.currently_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/*
 * File: BillFile.java
 * Description: JPA entity for Bills Vault PDF metadata and encrypted file bytes stored in the Database.
 * Project: Currently
 * Author: Liam Connell
 *
 */
@Entity
@Table(name = "bill_files")
public class BillFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many bills belong to one user
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    // Security metadata: SHA-256 hash records file integrity information; it is not the encrypted PDF content.
    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    // Vault data: encrypted PDF bytes are stored in the Database and decrypted only for download.
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "data", nullable = false)
    private byte[] data;

    public BillFile() {}

    // Standard JPA getters/setters used by repositories and services.
    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
}
