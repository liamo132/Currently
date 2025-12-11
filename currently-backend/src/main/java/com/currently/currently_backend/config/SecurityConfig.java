/*
 * File: SecurityConfig.java
 * Description: Spring Security configuration for JWT authentication, CORS, and stateless sessions.
 *              Registers JwtAuthenticationFilter to validate tokens for private endpoints.
 * Author: Liam Connell
 * Date: 2025-12-01
 */

package com.currently.currently_backend.config;

import com.currently.currently_backend.config.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    /*
     * Bean: securityFilterChain
     * Purpose:
     *   - Configure Spring Security to use JWT authentication
     *   - Allow public access to /api/auth and /api/appliances
     *   - Protect all other endpoints
     *   - Register the JwtAuthenticationFilter so incoming requests
     *     are authenticated based on the Authorization header.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {

        http
                // Enable CORS for frontend and disable CSRF (API-style usage)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public: authentication endpoints
                        .requestMatchers("/api/auth/**").permitAll()

                        // Public: appliance catalogue
                        .requestMatchers("/api/appliances/**").permitAll()

                        // Everything else requires JWT
                        .anyRequest().authenticated()
                )

                // Register custom JWT filter BEFORE Spring's username/password filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Stateless (no sessions)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

    /*
     * Bean: corsConfigurationSource
     * Purpose:
     *   Allow the Vite dev server to call backend APIs.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        cfg.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));

        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /*
     * Bean: passwordEncoder
     * Purpose:
     *   BCrypt hashing for user passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /*
     * Bean: authenticationManager
     * Purpose:
     *   Required for login in AuthController (authenticationManager.authenticate).
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
