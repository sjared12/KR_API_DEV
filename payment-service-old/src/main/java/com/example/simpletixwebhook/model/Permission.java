package com.example.simpletixwebhook.model;

/**
 * Permission enum for role-based access control in the subscription management system.
 * Defines all available permissions that can be assigned to users.
 */
public enum Permission {
    // Subscription Management Permissions
    VIEW_SUBSCRIPTIONS("view_subscriptions", "Can view subscription details"),
    CANCEL_SUBSCRIPTIONS("cancel_subscriptions", "Can cancel subscriptions"),
    
    // Refund Management Permissions
    REQUEST_REFUNDS("request_refunds", "Can request refunds"),
    APPROVE_REFUNDS("approve_refunds", "Can approve refunds for processing"),
    VIEW_REFUNDS("view_refunds", "Can view refund requests and history"),
    
    // User Management Permissions
    MANAGE_USERS("manage_users", "Can create, update, and manage users"),
    VIEW_USERS("view_users", "Can view user list and details"),
    
    // Administrative Permissions
    SYSTEM_ADMIN("system_admin", "Full system administrator access");

    private final String code;
    private final String description;

    Permission(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
