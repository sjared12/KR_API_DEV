package com.krhscougarband.paymentportal.services;

import com.krhscougarband.paymentportal.repositories.RevokedTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service to clean up expired revoked tokens from the database
 * Runs daily at 3 AM to remove tokens that have already expired
 */
@Service
public class TokenCleanupService {

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    /**
     * Clean up expired tokens from revoked_tokens table
     * Runs daily at 3 AM server time
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime cutoffTime = LocalDateTime.now();
        revokedTokenRepository.deleteExpiredTokens(cutoffTime);
        System.out.println("Cleaned up expired tokens older than: " + cutoffTime);
    }
}
