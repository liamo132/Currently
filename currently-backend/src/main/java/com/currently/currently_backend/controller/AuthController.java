/*
 * File: AuthController.java
 * Description: Exposes REST API endpoints for user registration and login.
 * Author: Liam Connell
 * Date: 2025-11-11
 *
 * Note: Some configuration and environment setup for this module took significant time to resolve,
 * including dependency alignment, Maven configuration, and security compatibility issues.
 * These complexities are now stabilized in this working version.
 */

package com.currently.currently_backend.controller;

import com.currently.currently_backend.model.User;
import com.currently.currently_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for authentication and registration.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // Endpoint: POST /api/auth/register
    // Purpose: Register a new user and return a JWT token or error message
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody User user) {
        String response = userService.registerUser(user);
        if (response.startsWith("Error")) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok("Registration successful. Token: " + response);
    }

    // Endpoint: POST /api/auth/login
    // Purpose: Authenticate a user and return a JWT token
    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@RequestParam String email, @RequestParam String password) {
        String response = userService.loginUser(email, password);
        if (response.startsWith("Error")) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok("Login successful. Token: " + response);
    }
}
