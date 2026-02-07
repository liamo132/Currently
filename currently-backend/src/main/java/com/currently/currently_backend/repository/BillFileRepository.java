package com.currently.currently_backend.repository;

import com.currently.currently_backend.model.BillFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillFileRepository extends JpaRepository<BillFile, Long> {
    List<BillFile> findAllByUserIdOrderByUploadedAtDesc(Long userId);
    Optional<BillFile> findByIdAndUserId(Long id, Long userId);
}
