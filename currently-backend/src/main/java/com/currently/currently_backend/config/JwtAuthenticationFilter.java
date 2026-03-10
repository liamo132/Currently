package com.currently.currently_backend.config;

import com.currently.currently_backend.model.User;
import com.currently.currently_backend.repository.UserRepository;
import com.currently.currently_backend.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final com.currently.currently_backend.service.UserLookupHashService userLookupHashService;

    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            UserRepository userRepository,
            com.currently.currently_backend.service.UserLookupHashService userLookupHashService
    ) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.userLookupHashService = userLookupHashService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();

        // Defensive cleanup for malformed client headers.
        token = token.replace("\"", "");
        if (token.startsWith("{") && token.contains("token")) {
            int colon = token.indexOf(':');
            int endBrace = token.lastIndexOf('}');
            if (colon != -1 && endBrace != -1 && endBrace > colon) {
                String maybe = token.substring(colon + 1, endBrace).trim();
                token = maybe.replace("{", "").replace("}", "").replace("\"", "").trim();
            }
        }

        try {
            String email = jwtUtil.extractUsername(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                String emailHash = userLookupHashService.emailHash(email);
                User user = userRepository.findByEmailHash(emailHash)
                        
                        .orElse(null);

                if (user != null && jwtUtil.validateToken(token, email)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException e) {
            SecurityContextHolder.clearContext();
            log.debug("Invalid JWT for {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        }

        chain.doFilter(request, response);
    }
}

