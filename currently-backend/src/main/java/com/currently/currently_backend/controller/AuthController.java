package com.currently.currently_backend.controller;

import com.currently.currently_backend.model.User;
import com.currently.currently_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerUser(@RequestBody User user) {
        String token = userService.registerUser(user);
        if (token.startsWith("Error")) {
            return ResponseEntity.badRequest().body(Map.of("error", token));
        }
        return ResponseEntity.ok(Map.of("token", token));
    }

    // POST /api/auth/login
    // Body: { "email": "...", "password": "..." }
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        String token = userService.loginUser(email, password);
        if (token.startsWith("Error")) {
            return ResponseEntity.badRequest().body(Map.of("error", token));
        }
        return ResponseEntity.ok(Map.of("token", token));
    }
}
