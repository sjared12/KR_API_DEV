package com.krhscougarband.paymentportal.security;

import org.springframework.security.core.Authentication;

public final class AuthRoleMapper {
    private AuthRoleMapper() {}

    public static String determineRole(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
        boolean isRefundApprover = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_REFUND_APPROVER"));
        if (isAdmin) return "ADMIN";
        if (isRefundApprover) return "REFUND_APPROVER";
        return "USER";
    }
}
