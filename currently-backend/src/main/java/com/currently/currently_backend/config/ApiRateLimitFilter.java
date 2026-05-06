package com.currently.currently_backend.config;

import com.currently.currently_backend.exception.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Deque<Long>> requestBuckets = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMillis;
    private final ObjectMapper objectMapper;
    private final boolean trustForwardHeaders;

    public ApiRateLimitFilter(
            @Value("${security.rate-limit.max-requests:30}") int maxRequests,
            @Value("${security.rate-limit.window-seconds:60}") int windowSeconds,
            @Value("${security.rate-limit.trust-forward-headers:false}") boolean trustForwardHeaders,
            ObjectMapper objectMapper
    ) {
        this.maxRequests = maxRequests;
        this.windowMillis = Duration.ofSeconds(windowSeconds).toMillis();
        this.trustForwardHeaders = trustForwardHeaders;
        this.objectMapper = objectMapper;
    }

    // Security filter: only applies rate limiting to high-risk Authentication and Bills Vault endpoints.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/api/auth/") || path.startsWith("/api/vault/"));
    }

    /*
     * Security filter: Rate limiting
     * Purpose: Tracks request timestamps per client IP and endpoint, rejects requests over the configured limit,
     * and returns a consistent JSON API error instead of an HTML error page.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String clientIp = extractClientIp(request);
        String key = clientIp + ":" + request.getRequestURI();
        long now = System.currentTimeMillis();

        Deque<Long> bucket = requestBuckets.computeIfAbsent(key, ignored -> new ArrayDeque<>());

        synchronized (bucket) {
            while (!bucket.isEmpty() && (now - bucket.peekFirst()) > windowMillis) {
                bucket.pollFirst();
            }

            if (bucket.size() >= maxRequests) {
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                ApiErrorResponse error = new ApiErrorResponse(
                        "RATE_LIMITED",
                        "Too many requests. Try again shortly.",
                        null,
                        Instant.now(),
                        request.getRequestURI()
                );
                response.getWriter().write(objectMapper.writeValueAsString(error));
                return;
            }

            bucket.addLast(now);
        }

        filterChain.doFilter(request, response);
    }

    // Security helper: chooses either remote address or trusted X-Forwarded-For depending on deployment config.
    private String extractClientIp(HttpServletRequest request) {
        if (!trustForwardHeaders) {
            return request.getRemoteAddr();
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] ips = forwarded.split(",");
            return ips[0].trim();
        }
        return request.getRemoteAddr();
    }
}
