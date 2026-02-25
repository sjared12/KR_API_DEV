package com.krhscougarband.paymentportal.repositories;

import com.krhscougarband.paymentportal.entities.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserEmail(String userEmail);
    Optional<UserProfile> findByUserId(String userId);
}
