package com.krhscougarband.paymentportal.security;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 10; // 10 requests
    private static final long TIME_WINDOW = 60_000; // per minute
    
    private final Map<String, RateLimitData> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        
        // Only rate limit auth endpoints
        if (path.contains("/api/auth/login") || path.contains("/api/auth/register")) {
            String clientIP = getClientIP(request);
            String key = clientIP + ":" + path;

            RateLimitData data = requestCounts.computeIfAbsent(key, k -> new RateLimitData());
            
            long currentTime = System.currentTimeMillis();
            
            // Reset if time window has passed
            if (currentTime - data.windowStart > TIME_WINDOW) {
                data.reset(currentTime);
            }
            
            if (data.count.incrementAndGet() > MAX_REQUESTS) {
                response.setStatus(429); // Too Many Requests
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        return request.getRemoteAddr();
    }

    private static class RateLimitData {
        AtomicInteger count = new AtomicInteger(0);
        long windowStart = System.currentTimeMillis();

        void reset(long newStart) {
            count.set(0);
            windowStart = newStart;
        }
    }
}
