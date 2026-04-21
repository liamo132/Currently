/*
 * File: UserApplianceRepository.java
 * Description: Spring Data JPA repository for managing UserAppliance entities.
 * Author: Liam Connell
 * Date: 2025-12-01
 */

package com.currently.currently_backend.repository;

import com.currently.currently_backend.model.User;
import com.currently.currently_backend.model.UserAppliance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Interface: UserApplianceRepository
 * Purpose: Provide database operations for UserAppliance entities.
 */
public interface UserApplianceRepository extends JpaRepository<UserAppliance, Long> {

    // Fetch the optional room in the same database round trip so DTO mapping
    // does not accidentally trigger one extra query per appliance.
    @EntityGraph(attributePaths = "room")
    List<UserAppliance> findByUserOrderByCreatedAtAsc(User user);
}
