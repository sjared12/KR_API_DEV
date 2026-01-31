package com.example.simpletixwebhook.controller.dto;

/**
 * DTO for user authentication
 */
public record AuthRequest(
    String username,
    String password
) {}
