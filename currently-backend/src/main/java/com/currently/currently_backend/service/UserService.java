package com.currently.currently_backend.service;

import com.currently.currently_backend.model.User;
import com.currently.currently_backend.repository.UserRepository;
import com.currently.currently_backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserLookupHashService userLookupHashService;
    private final SecurityAuditService securityAuditService;
    private final SecurityLockoutService securityLockoutService;

    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @Lazy AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       UserLookupHashService userLookupHashService,
                       SecurityAuditService securityAuditService,
                       SecurityLockoutService securityLockoutService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userLookupHashService = userLookupHashService;
        this.securityAuditService = securityAuditService;
        this.securityLockoutService = securityLockoutService;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String emailHash = userLookupHashService.emailHash(email);
        return userRepository.findByEmailHash(emailHash)
                
                .map(user -> {
                    if (user.getEmailHash() == null || user.getUsernameHash() == null) {
                        refreshUserHashes(user);
                        userRepository.save(user);
                    }
                    return user;
                })
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    public String registerUser(User user) {

        if (user == null) {
            throw new IllegalArgumentException("Missing request body.");
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }

        // IMPORTANT: this is the handle field, not Spring Security getUsername()
        if (user.getUsernameField() == null || user.getUsernameField().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }

        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }

        String trimmedEmail = user.getEmail().trim();
        String trimmedUsername = user.getUsernameField().trim();

        if (userRepository.existsByEmailHash(userLookupHashService.emailHash(trimmedEmail))) {
            throw new IllegalArgumentException("Email already in use.");
        }

        if (userRepository.existsByUsernameHash(userLookupHashService.usernameHash(trimmedUsername))) {
            throw new IllegalArgumentException("Username already in use.");
        }

        user.setEmail(trimmedEmail);
        user.setUsername(trimmedUsername);
        refreshUserHashes(user);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        securityAuditService.logRegistrationSuccess(trimmedEmail);

        return jwtUtil.generateToken(user.getEmail());
    }

    public String loginUser(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Email and password are required.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        securityLockoutService.assertLoginAllowed(normalizedEmail);

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email.trim(), password)
            );

            if (auth.isAuthenticated()) {
                securityLockoutService.recordLoginSuccess(normalizedEmail);
                securityAuditService.logLoginSuccess(normalizedEmail);
                return jwtUtil.generateToken(auth.getName());
            }
            throw new BadCredentialsException("Invalid credentials.");
        } catch (AuthenticationException e) {
            securityLockoutService.recordLoginFailure(normalizedEmail);
            securityAuditService.logLoginFailure(normalizedEmail, "invalid_credentials");
            throw new BadCredentialsException("Invalid email or password.");
        }
    }

    private void refreshUserHashes(User user) {
        user.setEmailHash(userLookupHashService.emailHash(user.getEmail()));
        user.setUsernameHash(userLookupHashService.usernameHash(user.getUsernameField()));
    }
}

