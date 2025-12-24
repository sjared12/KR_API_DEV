package com.example.logapi.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.Semaphore;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private final Semaphore permits;

    public RateLimitFilter(@Value("${rate.limit.permits:500}") int maxPermits) {
        this.permits = new Semaphore(Math.max(1, maxPermits));
        log.info("RateLimitFilter initialized with maxPermits={}", maxPermits);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws IOException, ServletException {
        if (!permits.tryAcquire()) {
            log.warn("Rate limit exceeded (429)");
            res.setStatus(429); // Too Many Requests
            return;
        }
        log.debug("Permit acquired, available permits: {}", permits.availablePermits());
        try {
            chain.doFilter(req, res);
        } finally {
            permits.release();
            log.debug("Permit released, available permits: {}", permits.availablePermits());
        }
    }
}
