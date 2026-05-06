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
    // Backend Query: legacy encrypted-field lookup by decrypted email where applicable.
    Optional<User> findByEmail(String email);

    // Backend Query: legacy encrypted-field lookup by decrypted username where applicable.
    Optional<User> findByUsername(String username);

    // Backend Query: Authentication lookup using deterministic HMAC email hash so encrypted email stays searchable.
    Optional<User> findByEmailHash(String emailHash);

    // Backend Query: checks usernames through HMAC hashes without exposing plaintext usernames to lookup indexes.
    Optional<User> findByUsernameHash(String usernameHash);

    // Backend Query: legacy uniqueness check for email values.
    boolean existsByEmail(String email);

    // Backend Query: legacy uniqueness check for username values.
    boolean existsByUsername(String username);

    // Backend Query: Register Validation check for duplicate email hash.
    boolean existsByEmailHash(String emailHash);

    // Backend Query: Register Validation check for duplicate username hash.
    boolean existsByUsernameHash(String usernameHash);
}
