package com.example.simpletixwebhook.controller.dto;

import com.example.simpletixwebhook.model.Permission;
import java.util.Set;

/**
 * DTO for updating user
 */
public record UpdateUserRequest(
    String email,
    String fullName,
    Set<Permission> permissions
) {}
