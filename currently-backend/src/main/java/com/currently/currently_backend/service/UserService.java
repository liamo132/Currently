package com.currently.currently_backend.service;

import com.currently.currently_backend.model.User;
import com.currently.currently_backend.repository.UserRepository;
import com.currently.currently_backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @Lazy AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    public String registerUser(User user) {

        if (user == null) {
            return "Error: Missing request body.";
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return "Error: Email is required.";
        }

        // IMPORTANT: this is the handle field, not Spring Security getUsername()
        if (user.getUsernameField() == null || user.getUsernameField().trim().isEmpty()) {
            return "Error: Username is required.";
        }

        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return "Error: Password is required.";
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            return "Error: Email already in use.";
        }

        if (userRepository.existsByUsername(user.getUsernameField())) {
            return "Error: Username already in use.";
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        return jwtUtil.generateToken(user.getEmail());
    }

    public String loginUser(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return "Error: Email and password are required.";
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            if (auth.isAuthenticated()) {
                return jwtUtil.generateToken(email);
            } else {
                return "Error: Invalid credentials.";
            }
        } catch (AuthenticationException e) {
            return "Error: Invalid email or password.";
        }
    }
}
