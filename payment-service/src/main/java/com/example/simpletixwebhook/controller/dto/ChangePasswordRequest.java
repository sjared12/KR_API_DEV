package com.example.simpletixwebhook.controller.dto;

/**
 * DTO for password change
 */
public record ChangePasswordRequest(
    String oldPassword,
    String newPassword
) {}
