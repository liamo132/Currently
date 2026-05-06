package com.currently.currently_backend.repository;

import com.currently.currently_backend.model.BillFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/*
 * Repository: BillFileRepository
 * Purpose: Database access for encrypted Bills Vault PDF records owned by users.
 */
public interface BillFileRepository extends JpaRepository<BillFile, Long> {
    // Backend Query: lists bill metadata newest-first for the authenticated user's Bills Vault.
    List<BillFile> findAllByUserIdOrderByUploadedAtDesc(Long userId);

    // Backend Query: fetches one bill only when the file id and owner user id both match.
    Optional<BillFile> findByIdAndUserId(Long id, Long userId);
}
