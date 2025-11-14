/*
 * File: UserRepository.java
 * Description: Repository interface for performing CRUD operations on User entities.
 * Author: Liam Connell
 * Date: 2025-11-10
 */

package com.currently.currently_backend.repository;

import com.currently.currently_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Custom query methods automatically implemented by Spring Data JPA
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
