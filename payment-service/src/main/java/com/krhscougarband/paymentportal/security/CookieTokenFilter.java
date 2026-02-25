package com.krhscougarband.paymentportal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Component
public class CookieTokenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("AUTH_TOKEN".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token != null && !token.isEmpty()) {
            final String bearerToken = "Bearer " + token;
            HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if ("Authorization".equalsIgnoreCase(name)) {
                        return bearerToken;
                    }
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if ("Authorization".equalsIgnoreCase(name)) {
                        return Collections.enumeration(Collections.singletonList(bearerToken));
                    }
                    return super.getHeaders(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    Map<String, String> headers = new HashMap<>();
                    Enumeration<String> e = super.getHeaderNames();
                    while (e.hasMoreElements()) {
                        String name = e.nextElement();
                        headers.put(name, super.getHeader(name));
                    }
                    headers.put("Authorization", bearerToken);
                    return Collections.enumeration(headers.keySet());
                }
            };
            filterChain.doFilter(wrapper, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Skip filter for static resources and auth endpoints
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/") || 
               path.startsWith("/css/") || 
               path.startsWith("/js/") || 
               path.equals("/favicon.svg") || 
               path.startsWith("/api/auth/") ||
               path.equals("/shop") ||
               path.equals("/cart") ||
               path.equals("/orders") ||
               path.equals("/profile");
    }
}
