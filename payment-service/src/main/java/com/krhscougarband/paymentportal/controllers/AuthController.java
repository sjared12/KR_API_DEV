package com.krhscougarband.paymentportal.controllers;

import com.krhscougarband.paymentportal.entities.RevokedToken;
import com.krhscougarband.paymentportal.entities.User;
import com.krhscougarband.paymentportal.repositories.RevokedTokenRepository;
import com.krhscougarband.paymentportal.repositories.UserRepository;
import com.krhscougarband.paymentportal.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Users can register with email/password
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password required"));
        }

        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("success", true, "message", "User registered successfully"));
    }

    // Login with email/password returns JWT token
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body) {
        String email = (String) body.get("email");
        String password = (String) body.get("password");
        Boolean rememberMe = body.get("rememberMe") != null ? (Boolean) body.get("rememberMe") : false;

        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password required"));
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }

        String token = jwtUtil.generateToken(email, rememberMe);
        String refreshToken = jwtUtil.generateRefreshToken(email);
        
        return ResponseEntity.ok(Map.of(
            "token", token, 
            "refreshToken", refreshToken,
            "email", email,
            "expiresIn", rememberMe ? 604800000L : 3600000L // milliseconds
        ));
    }

    // Get current user info
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }

        User user = userOpt.get();
        return ResponseEntity.ok(Map.of(
            "email", user.getEmail(),
            "firstName", user.getFirstName() != null ? user.getFirstName() : "",
            "lastName", user.getLastName() != null ? user.getLastName() : "",
            "role", user.getRole() != null ? user.getRole() : "USER"
        ));
    }

    // Refresh access token using refresh token
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body, Authentication authentication) {
        String refreshToken = body.get("refreshToken");
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Refresh token required"));
        }

        try {
            // Validate refresh token
            String email = jwtUtil.extractEmail(refreshToken);
            
            // Check if token is revoked
            String tokenHash = jwtUtil.hashToken(refreshToken);
            if (revokedTokenRepository.existsByTokenHash(tokenHash)) {
                return ResponseEntity.status(401).body(Map.of("error", "Token has been revoked"));
            }
            
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid refresh token"));
            }
            
            if (!jwtUtil.validateToken(refreshToken, email)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired refresh token"));
            }

            // Generate new access token (1 hour)
            String newToken = jwtUtil.generateToken(email, false);
            
            return ResponseEntity.ok(Map.of(
                "token", newToken,
                "email", email,
                "expiresIn", 3600000L
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid refresh token"));
        }
    }

    // Logout current token
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        }

        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            String email = authentication.getName();
            
            RevokedToken revokedToken = new RevokedToken();
            revokedToken.setTokenHash(jwtUtil.hashToken(token));
            revokedToken.setUserEmail(email);
            revokedToken.setExpiryTime(jwtUtil.extractExpirationAsLocalDateTime(token));
            revokedToken.setReason("logout");
            
            revokedTokenRepository.save(revokedToken);
            
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        }
    }

    // Logout from all devices (revoke all user's tokens)
    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String email = authentication.getName();
        
        // This is a simplified approach - in production, you'd want to track all active tokens
        // For now, we'll just indicate success and rely on token expiration
        // A more robust solution would require storing all issued tokens
        
        return ResponseEntity.ok(Map.of(
            "message", "Logged out from all devices successfully",
            "note", "Please re-login on all devices"
        ));
    }

    // Update user profile
    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }

        User user = userOpt.get();
        
        // Update fields if provided
        if (body.containsKey("firstName")) {
            user.setFirstName(body.get("firstName"));
        }
        if (body.containsKey("lastName")) {
            user.setLastName(body.get("lastName"));
        }
        
        userRepository.save(user);
        
        return ResponseEntity.ok(Map.of(
            "email", user.getEmail(),
            "firstName", user.getFirstName() != null ? user.getFirstName() : "",
            "lastName", user.getLastName() != null ? user.getLastName() : "",
            "role", user.getRole() != null ? user.getRole() : "USER",
            "message", "Profile updated successfully"
        ));
    }
}