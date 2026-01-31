package com.krhscougarband.paymentportal.controllers;

import com.krhscougarband.paymentportal.dtos.UserDto;
import com.krhscougarband.paymentportal.entities.User;
import com.krhscougarband.paymentportal.repositories.UserRepository;
import com.krhscougarband.paymentportal.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // =========================
    // REGISTER
    // =========================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String password = body.get("password");
            String firstName = body.get("firstName");
            String lastName = body.get("lastName");

            if (email == null || email.isBlank() || password == null || password.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
            }

            if (userRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User already exists"));
            }

            User user = new User();
            user.setEmail(email.trim());
            user.setPassword(passwordEncoder.encode(password));
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setRole("USER"); // default role

            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "User registered"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Registration failed"));
        }
    }

    // =========================
    // LOGIN
    // =========================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        // Gracefully handle missing user or password mismatch to avoid 500s
        return userRepository.findByEmail(email)
            .filter(u -> u.getPassword() != null && passwordEncoder.matches(password, u.getPassword()))
            .<ResponseEntity<?>>map(user -> {
                String token = jwtUtil.generateToken(user.getEmail());
                return ResponseEntity.ok(Map.of(
                    "token", token,
                    "role", user.getRole()
                ));
            })
            .orElseGet(() -> ResponseEntity.status(401)
                .body(Map.of("error", "Invalid credentials")));
    }

    // =========================
    // CURRENT USER (optional)
    // =========================
    @GetMapping("/me")
    public ResponseEntity<?> me(org.springframework.security.core.Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        User user = userRepository.findByEmailWithStudents(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(new UserDto(user));
    }

    // =========================
    // PASSWORD RESET
    // =========================
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestAttribute("email") String email,
            @RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "New password must be at least 6 characters"));
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Current password is incorrect"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
