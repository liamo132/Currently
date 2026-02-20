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
        String token;
        final String email;

        System.out.println("...");
        System.out.println("🌐 Request: " + request.getMethod() + " " + request.getRequestURI());
        System.out.println("🔑 Auth Header: " + (authHeader != null
                ? authHeader.substring(0, Math.min(60, authHeader.length())) + "..."
                : "MISSING"));

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        token = authHeader.substring(7).trim();

        // ---- Defensive cleanup: remove quotes / accidental JSON wrapping ----
        // Cases seen in the wild:
        // Bearer "eyJ..."
        // Bearer {"token":"eyJ..."}
        token = token.replace("\"", "");

        if (token.startsWith("{") && token.contains("token")) {
            // very small extraction without adding JSON dependency here
            int idx = token.indexOf("token:");
            if (idx == -1) idx = token.indexOf("token");
            // If it's JSON-ish, try a safer approach:
            // {"token":"<jwt>"} -> extract between first ':' and last '}'
            int colon = token.indexOf(':');
            int endBrace = token.lastIndexOf('}');
            if (colon != -1 && endBrace != -1 && endBrace > colon) {
                String maybe = token.substring(colon + 1, endBrace).trim();
                // remove any remaining braces/quotes
                maybe = maybe.replace("{", "").replace("}", "").replace("\"", "").trim();
                token = maybe;
            }
        }
        // -------------------------------------------------------------------

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
                                    user,
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
