package com.example.simpletixwebhook.service;

import com.example.simpletixwebhook.model.Permission;
import com.example.simpletixwebhook.model.User;
import com.example.simpletixwebhook.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for user authentication and management with permission-based access control.
 */
@Service
@Transactional
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticate user by username and password
     */
    public Optional<User> authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            log.warn("Authentication failed: user '{}' not found", username);
            return Optional.empty();
        }
        
        User user = userOpt.get();
        
        if (!user.isActive()) {
            log.warn("Authentication failed: user '{}' is inactive", username);
            return Optional.empty();
        }
        
        boolean passwordMatches = passwordEncoder.matches(password, user.getPasswordHash());
        log.debug("Password verification for user '{}': {} (provided: '{}', stored: '{}')", 
            username, passwordMatches, password, user.getPasswordHash());
        
        if (!passwordMatches) {
            log.warn("Authentication failed: invalid password for user '{}'", username);
            return Optional.empty();
        }
        
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        log.info("User '{}' authenticated successfully", username);
        return Optional.of(user);
    }

    /**
     * Create a new user with encoded password
     */
    public User createUser(String username, String password, String email, String fullName, Set<Permission> permissions) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        String passwordHash = passwordEncoder.encode(password);
        User user = new User(username, passwordHash, email, fullName);
        user.setPermissions(permissions != null ? permissions : new HashSet<>());
        user.setLastLogin(LocalDateTime.now());

        User saved = userRepository.save(user);
        log.info("New user created: {}", username);
        return saved;
    }

    /**
     * Get user by ID
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Get user by username
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Update user
     */
    public User updateUser(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        log.info("User {} updated", user.getUsername());
        return saved;
    }

    /**
     * Change user password
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getUsername());
    }

    /**
     * Reset user password (admin function)
     */
    public void resetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password reset for user: {}", user.getUsername());
    }

    /**
     * Add permission to user
     */
    public void addPermission(Long userId, Permission permission) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.addPermission(permission);
        userRepository.save(user);
        log.info("Permission {} added to user {}", permission, user.getUsername());
    }

    /**
     * Remove permission from user
     */
    public void removePermission(Long userId, Permission permission) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.removePermission(permission);
        userRepository.save(user);
        log.info("Permission {} removed from user {}", permission, user.getUsername());
    }

    /**
     * Check if user has permission
     */
    public boolean hasPermission(Long userId, Permission permission) {
        return userRepository.findById(userId)
                .map(user -> user.hasPermission(permission))
                .orElse(false);
    }

    /**
     * Disable user (soft delete)
     */
    public void disableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setActive(false);
        userRepository.save(user);
        log.info("User {} disabled", user.getUsername());
    }

    /**
     * Enable user
     */
    public void enableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setActive(true);
        userRepository.save(user);
        log.info("User {} enabled", user.getUsername());
    }

    /**
     * Delete user permanently
     */
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        userRepository.delete(user);
        log.info("User {} deleted", user.getUsername());
    }


}
