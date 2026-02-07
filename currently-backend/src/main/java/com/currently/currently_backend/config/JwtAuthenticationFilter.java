/*
 * File: JwtAuthenticationFilter.java
 * Description: Reads JWT from Authorization header, validates it, and sets the
 *              authenticated user in Spring Security's context.
 * Author: Liam Connell
 * Date: 2025-12-01
 */

package com.currently.currently_backend.config;

import com.currently.currently_backend.util.JwtUtil;
import com.currently.currently_backend.model.User;
import com.currently.currently_backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String token;
        final String email;

        // 🔍 LOG THE REQUEST
        System.out.println("...");
        System.out.println("🌐 Request: " + request.getMethod() + " " + request.getRequestURI());
        System.out.println("🔑 Auth Header: " + (authHeader != null ? authHeader.substring(0, Math.min(30, authHeader.length())) + "..." : "MISSING"));

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("❌ No Bearer token - allowing request to proceed");
            chain.doFilter(request, response);
            return;
        }

        token = authHeader.substring(7);

        try {
            email = jwtUtil.extractUsername(token);
            System.out.println("📧 Extracted Email: " + email);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                User user = userRepository.findByEmail(email).orElse(null);
                System.out.println("👤 User Found: " + (user != null ? user.getEmail() : "NULL"));

                if (user != null && jwtUtil.validateToken(token, email)) {
                    System.out.println("✅ Token VALID - Setting Authentication");

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    user,  // ✅ CHANGED: Store full User object
                                    null,
                                    user.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    System.out.println("❌ Token INVALID or User NULL");
                }
            } else if (email == null) {
                System.out.println("❌ Email extraction failed");
            } else {
                System.out.println("ℹ️ Authentication already set");
            }
        } catch (Exception e) {
            System.err.println("💥 Exception in JWT Filter: " + e.getMessage());
            e.printStackTrace();
        }

        chain.doFilter(request, response);
    }
}