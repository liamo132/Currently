package com.currently.currently_backend.controller;

import com.currently.currently_backend.dto.auth.LoginRequest;
import com.currently.currently_backend.dto.auth.RegisterRequest;
import com.currently.currently_backend.dto.auth.TokenResponse;
import com.currently.currently_backend.model.User;
import com.currently.currently_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /*
     * Controller API: Register
     * Purpose: Accepts a new account request, delegates Validation and password hashing to UserService,
     * and returns a JWT token so the frontend can immediately enter authenticated pages.
     */
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        String token = userService.registerUser(user);
        return ResponseEntity.ok(new TokenResponse(token));
    }

    /*
     * Controller API: Login
     * Purpose: Receives email/password credentials, relies on Spring Security authentication,
     * and returns a signed JWT when the user is valid.
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        String token = userService.loginUser(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(new TokenResponse(token));
    }
}
