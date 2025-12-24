package com.example.logapi.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ReadListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Objects;

@Component
public class HmacFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HmacFilter.class);
    private final String secret;
    private final boolean enabled;

    public HmacFilter(@Value("${hmac.secret:}") String secret,
                      @Value("${hmac.enabled:true}") boolean enabled) {
        this.secret = secret;
        this.enabled = enabled;
        log.info("HmacFilter initialized: enabled={}, secretLength={}", enabled, secret == null ? 0 : secret.length());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {

        log.debug("HmacFilter: enabled={}", enabled);
        if (!enabled) {
            log.debug("HMAC verification disabled, skipping");
            chain.doFilter(request, response);
            return;
        }

        if (secret == null || secret.isEmpty()) {
            log.error("HMAC secret is not configured");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        String signature = request.getHeader("X-Signature");
        if (signature == null) {
            log.warn("X-Signature header missing");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        byte[] body = request.getInputStream().readAllBytes();
        String computed = hmac(body);
        log.debug("Computed HMAC: {}, received signature length: {}", computed.substring(0, Math.min(10, computed.length())) + "...", signature.length());

        if (!MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8), computed.getBytes(StandardCharsets.UTF_8))) {
            log.warn("HMAC signature mismatch");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        log.debug("HMAC signature verified");
        chain.doFilter(new CachedBodyHttpServletRequest(request, body), response);
    }

    private String hmac(byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(body));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        CachedBodyHttpServletRequest(HttpServletRequest request, byte[] cachedBody) {
            super(request);
            this.cachedBody = Objects.requireNonNullElseGet(cachedBody, () -> new byte[0]);
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return byteStream.read();
                }

                @Override
                public boolean isFinished() {
                    return byteStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // no-op: synchronous request body
                }
            };
        }

        @Override
        public java.io.BufferedReader getReader() {
            return new java.io.BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
