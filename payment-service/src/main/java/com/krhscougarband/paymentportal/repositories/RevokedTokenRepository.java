package com.krhscougarband.paymentportal.repositories;

import com.krhscougarband.paymentportal.entities.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, UUID> {
    
    boolean existsByTokenHash(String tokenHash);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM RevokedToken rt WHERE rt.userEmail = :email")
    void deleteByUserEmail(String email);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM RevokedToken rt WHERE rt.expiryTime < :cutoffTime")
    void deleteExpiredTokens(LocalDateTime cutoffTime);
}
