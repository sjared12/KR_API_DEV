package com.krhscougarband.paymentportal.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "revoked_tokens", indexes = {
    @Index(name = "idx_token_hash", columnList = "tokenHash"),
    @Index(name = "idx_expiry_time", columnList = "expiryTime")
})
@Data
public class RevokedToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash; // SHA-256 hash of the JWT token
    
    @Column(nullable = false)
    private String userEmail;
    
    @Column(nullable = false)
    private LocalDateTime revokedAt;
    
    @Column(nullable = false)
    private LocalDateTime expiryTime; // When the token would have expired naturally
    
    private String reason; // Optional: "logout", "logout_all", "security", etc.
    
    public RevokedToken() {
        this.revokedAt = LocalDateTime.now();
    }
}
