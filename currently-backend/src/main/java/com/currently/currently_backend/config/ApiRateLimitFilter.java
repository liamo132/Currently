package com.currently.currently_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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

    public ApiRateLimitFilter(
            @Value("${security.rate-limit.max-requests:30}") int maxRequests,
            @Value("${security.rate-limit.window-seconds:60}") int windowSeconds
    ) {
        this.maxRequests = maxRequests;
        this.windowMillis = Duration.ofSeconds(windowSeconds).toMillis();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/api/auth/") || path.startsWith("/api/vault/"));
    }

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
                response.getWriter().write("{\"error\":\"Too many requests. Try again shortly.\"}");
                return;
            }

            bucket.addLast(now);
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] ips = forwarded.split(",");
            return ips[0].trim();
        }
        return request.getRemoteAddr();
    }
}
