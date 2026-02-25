package com.krhscougarband.paymentportal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * This filter is no longer needed with local JWT authentication.
 * User syncing is now handled by AuthController on login/register.
 * Kept as placeholder for backward compatibility.
 */
public class KeycloakUserSyncFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // No-op: Users are managed locally via AuthController
        filterChain.doFilter(request, response);
    }
}
