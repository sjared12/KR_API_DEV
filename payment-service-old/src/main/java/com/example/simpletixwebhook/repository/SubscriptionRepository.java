package com.example.simpletixwebhook.repository;

import com.example.simpletixwebhook.model.Subscription;
import com.example.simpletixwebhook.model.Subscription.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Subscription entity with custom queries for subscription management.
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    /**
     * Find subscription by Square subscription ID
     */
    Optional<Subscription> findBySquareSubscriptionId(String squareSubscriptionId);

    /**
     * Find all subscriptions for a customer
     */
    List<Subscription> findBySquareCustomerId(String squareCustomerId);

    /**
     * Find subscriptions by status
     */
    List<Subscription> findByStatus(SubscriptionStatus status);

    /**
     * Find subscriptions by customer email
     */
    List<Subscription> findByCustomerEmail(String customerEmail);

    /**
     * Find active subscriptions
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE'")
    Page<Subscription> findActiveSubscriptions(Pageable pageable);

    /**
     * Find subscriptions by status with pagination
     */
    Page<Subscription> findByStatus(SubscriptionStatus status, Pageable pageable);

    /**
     * Find subscriptions by customer email with pagination
     */
    Page<Subscription> findByCustomerEmail(String customerEmail, Pageable pageable);

    /**
     * Search subscriptions by customer email or subscription ID
     */
    @Query("SELECT s FROM Subscription s WHERE " +
           "LOWER(s.customerEmail) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.squareSubscriptionId) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.squareCustomerId) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Subscription> searchSubscriptions(@Param("query") String query, Pageable pageable);
}
