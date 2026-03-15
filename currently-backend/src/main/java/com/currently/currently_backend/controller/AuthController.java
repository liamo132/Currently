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

    // POST /api/auth/register
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

    // POST /api/auth/login
    // Body: { "email": "...", "password": "..." }
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        String token = userService.loginUser(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(new TokenResponse(token));
    }
}
