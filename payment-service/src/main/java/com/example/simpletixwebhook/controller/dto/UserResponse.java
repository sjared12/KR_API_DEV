package com.example.simpletixwebhook.controller.dto;

import com.example.simpletixwebhook.model.Permission;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for user response
 */
public record UserResponse(
    Long id,
    String username,
    String email,
    String fullName,
    boolean active,
    Set<Permission> permissions,
    LocalDateTime createdAt,
    LocalDateTime lastLogin
) {}
