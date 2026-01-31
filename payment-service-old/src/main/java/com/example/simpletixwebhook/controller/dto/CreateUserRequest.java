package com.example.simpletixwebhook.controller.dto;

import com.example.simpletixwebhook.model.Permission;
import java.util.Set;

/**
 * DTO for creating a new user
 */
public record CreateUserRequest(
    String username,
    String password,
    String email,
    String fullName,
    Set<Permission> permissions
) {}
