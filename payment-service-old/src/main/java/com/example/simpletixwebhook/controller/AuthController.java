package com.example.simpletixwebhook.controller;

import com.example.simpletixwebhook.controller.dto.AuthRequest;
import com.example.simpletixwebhook.controller.dto.UserResponse;
import com.example.simpletixwebhook.controller.dto.CreateUserRequest;
import com.example.simpletixwebhook.controller.dto.UpdateUserRequest;
import com.example.simpletixwebhook.controller.dto.ChangePasswordRequest;
import com.example.simpletixwebhook.model.Permission;
import com.example.simpletixwebhook.model.User;
import com.example.simpletixwebhook.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * REST Controller for user authentication and management
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Login endpoint - authenticate user with username/password
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            Optional<User> user = userService.authenticate(request.username(), request.password());

            if (user.isPresent()) {
                User authenticatedUser = user.get();
                UserResponse response = new UserResponse(
                    authenticatedUser.getId(),
                    authenticatedUser.getUsername(),
                    authenticatedUser.getEmail(),
                    authenticatedUser.getFullName(),
                    authenticatedUser.isActive(),
                    authenticatedUser.getPermissions(),
                    authenticatedUser.getCreatedAt(),
                    authenticatedUser.getLastLogin()
                );
                log.info("User {} logged in successfully", request.username());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Login failed for user: {}", request.username());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid username or password"));
            }
        } catch (Exception e) {
            log.error("Login error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Login failed: " + e.getMessage()));
        }
    }

    /**
     * Get current user information
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // In a real implementation, extract user from JWT token
            // For now, this is a placeholder
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(Map.of("message", "JWT token support needed"));
        } catch (Exception e) {
            log.error("Error getting current user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting user: " + e.getMessage()));
        }
    }

    /**
     * Create a new user (admin only)
     */
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        try {
            if (request.username() == null || request.username().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
            }
            if (request.password() == null || request.password().length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
            }

            User user = userService.createUser(
                request.username(),
                request.password(),
                request.email(),
                request.fullName(),
                request.permissions() != null ? request.permissions() : Set.of(Permission.VIEW_SUBSCRIPTIONS)
            );

            UserResponse response = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.isActive(),
                user.getPermissions(),
                user.getCreatedAt(),
                user.getLastLogin()
            );

            log.info("New user created: {}", request.username());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("User creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creating user: " + e.getMessage()));
        }
    }

    /**
     * Get user by ID
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        try {
            Optional<User> user = userService.getUserById(id);

            if (user.isPresent()) {
                User u = user.get();
                UserResponse response = new UserResponse(
                    u.getId(),
                    u.getUsername(),
                    u.getEmail(),
                    u.getFullName(),
                    u.isActive(),
                    u.getPermissions(),
                    u.getCreatedAt(),
                    u.getLastLogin()
                );
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting user: " + e.getMessage()));
        }
    }

    /**
     * Get all users
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            List<UserResponse> responses = users.stream()
                    .map(u -> new UserResponse(
                        u.getId(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getFullName(),
                        u.isActive(),
                        u.getPermissions(),
                        u.getCreatedAt(),
                        u.getLastLogin()
                    ))
                    .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting users: " + e.getMessage()));
        }
    }

    /**
     * Update user
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        try {
            User user = userService.getUserById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            if (request.email() != null && !request.email().isEmpty()) {
                user.setEmail(request.email());
            }
            if (request.fullName() != null && !request.fullName().isEmpty()) {
                user.setFullName(request.fullName());
            }
            if (request.permissions() != null) {
                user.setPermissions(request.permissions());
            }

            User updated = userService.updateUser(user);

            UserResponse response = new UserResponse(
                updated.getId(),
                updated.getUsername(),
                updated.getEmail(),
                updated.getFullName(),
                updated.isActive(),
                updated.getPermissions(),
                updated.getCreatedAt(),
                updated.getLastLogin()
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating user: " + e.getMessage()));
        }
    }

    /**
     * Change user password
     */
    @PostMapping("/users/{id}/change-password")
    public ResponseEntity<?> changePassword(@PathVariable Long id, @RequestBody ChangePasswordRequest request) {
        try {
            userService.changePassword(id, request.oldPassword(), request.newPassword());
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error changing password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error changing password: " + e.getMessage()));
        }
    }

    /**
     * Reset user password (admin only)
     */
    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String newPassword = request.get("newPassword");
            if (newPassword == null || newPassword.length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
            }

            userService.resetPassword(id, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        } catch (Exception e) {
            log.error("Error resetting password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error resetting password: " + e.getMessage()));
        }
    }

    /**
     * Disable user
     */
    @PostMapping("/users/{id}/disable")
    public ResponseEntity<?> disableUser(@PathVariable Long id) {
        try {
            userService.disableUser(id);
            return ResponseEntity.ok(Map.of("message", "User disabled successfully"));
        } catch (Exception e) {
            log.error("Error disabling user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error disabling user: " + e.getMessage()));
        }
    }

    /**
     * Enable user
     */
    @PostMapping("/users/{id}/enable")
    public ResponseEntity<?> enableUser(@PathVariable Long id) {
        try {
            userService.enableUser(id);
            return ResponseEntity.ok(Map.of("message", "User enabled successfully"));
        } catch (Exception e) {
            log.error("Error enabling user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error enabling user: " + e.getMessage()));
        }
    }

    /**
     * Delete user
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error deleting user: " + e.getMessage()));
        }
    }

    /**
     * Get available permissions
     */
    @GetMapping("/permissions")
    public ResponseEntity<?> getAvailablePermissions() {
        List<Map<String, String>> permissions = Arrays.stream(Permission.values())
                .map(p -> Map.of(
                    "code", p.getCode(),
                    "name", p.toString(),
                    "description", p.getDescription()
                ))
                .toList();
        return ResponseEntity.ok(permissions);
    }


}
