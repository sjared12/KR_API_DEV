package com.krhscougarband.paymentportal.security;

import com.krhscougarband.paymentportal.repositories.RevokedTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final RevokedTokenRepository revokedTokenRepository;

    public JwtFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService, RevokedTokenRepository revokedTokenRepository) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        
        // Skip JWT processing for OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            final String authHeader = request.getHeader("Authorization");
            String email = null;
            String jwt = null;

            // ...existing code...

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
                
                // Only try to extract email if jwt is not empty
                if (jwt != null && !jwt.isEmpty()) {
                    try {
                        email = jwtUtil.extractEmail(jwt);
                    } catch (Exception e) {
                        // Invalid JWT token - just skip authentication
                        filterChain.doFilter(request, response);
                        return;
                    }
                }
            }

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    // Check if token is revoked
                    String tokenHash = jwtUtil.hashToken(jwt);
                    if (revokedTokenRepository.existsByTokenHash(tokenHash)) {
                        // Token is revoked, skip authentication
                        filterChain.doFilter(request, response);
                        return;
                    }
                    
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    
                    if (userDetails == null) {
                        filterChain.doFilter(request, response);
                        return;
                    }

                    if (jwtUtil.validateToken(jwt, email)) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        // ...existing code...
                    } else {
                        // ...existing code...
                    }
                } catch (Exception e) {
                    // ...existing code...
                }
            }
        } catch (Exception e) {
            // Log error but don't fail - let the request continue
            // Security will handle unauthorized access
        }

        filterChain.doFilter(request, response);
    }
}
