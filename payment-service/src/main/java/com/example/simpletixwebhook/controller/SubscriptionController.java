package com.example.simpletixwebhook.controller;

import com.example.simpletixwebhook.controller.dto.SubscriptionResponse;
import com.example.simpletixwebhook.controller.dto.CreateSubscriptionRequest;
import com.example.simpletixwebhook.controller.dto.CancelSubscriptionRequest;
import com.example.simpletixwebhook.controller.dto.UpdateSubscriptionAmountRequest;
import com.example.simpletixwebhook.model.Subscription;
import com.example.simpletixwebhook.model.Subscription.SubscriptionStatus;
import com.example.simpletixwebhook.service.SubscriptionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * REST Controller for subscription management
 */
@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "*")
public class SubscriptionController {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /**
     * Create a new subscription
     */
    @PostMapping
    public ResponseEntity<?> createSubscription(@RequestBody CreateSubscriptionRequest request) {
        try {
            Subscription subscription = subscriptionService.createSubscription(
                request.squareSubscriptionId(),
                request.squareCustomerId(),
                request.squarePlanId(),
                request.customerEmail(),
                request.amount()
            );

            if (request.currency() != null) {
                subscription.setCurrency(request.currency());
            }
            if (request.description() != null) {
                subscription.setDescription(request.description());
            }
            if (request.billingCycle() != null) {
                subscription.setBillingCycle(request.billingCycle());
            }
            if (request.amountPerPayment() != null) {
                subscription.setAmountPerPayment(request.amountPerPayment());
            }
            if (request.frequency() != null) {
                subscription.setFrequency(request.frequency());
            }
            if (request.estimatedEndDate() != null) {
                subscription.setEstimatedEndDate(java.time.LocalDateTime.parse(request.estimatedEndDate() + "T00:00:00"));
            }
            if (request.numberOfPayments() != null) {
                subscription.setNumberOfPayments(request.numberOfPayments());
            }

            subscriptionService.updateSubscriptionAmount(subscription.getId(), request.amount());
            subscription = subscriptionService.getSubscriptionById(subscription.getId()).orElse(subscription);

            SubscriptionResponse response = mapToResponse(subscription);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Subscription creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creating subscription: " + e.getMessage()));
        }
    }

    /**
     * Get subscription by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getSubscription(@PathVariable Long id) {
        try {
            Optional<Subscription> subscription = subscriptionService.getSubscriptionById(id);

            if (subscription.isPresent()) {
                return ResponseEntity.ok(mapToResponse(subscription.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting subscription: " + e.getMessage()));
        }
    }

    /**
     * Get subscription by Square ID
     */
    @GetMapping("/square/{squareId}")
    public ResponseEntity<?> getSubscriptionBySquareId(@PathVariable String squareId) {
        try {
            Optional<Subscription> subscription = subscriptionService.getSubscriptionBySquareId(squareId);

            if (subscription.isPresent()) {
                return ResponseEntity.ok(mapToResponse(subscription.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting subscription: " + e.getMessage()));
        }
    }

    /**
     * Get all subscriptions with pagination
     */
    @GetMapping
    public ResponseEntity<?> getAllSubscriptions(Pageable pageable) {
        try {
            Page<Subscription> subscriptions = subscriptionService.getAllSubscriptions(pageable);
            Page<SubscriptionResponse> responses = subscriptions.map(this::mapToResponse);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting subscriptions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting subscriptions: " + e.getMessage()));
        }
    }

    /**
     * Get subscriptions by customer
     */
    @GetMapping("/customer/{email}")
    public ResponseEntity<?> getSubscriptionsByCustomer(@PathVariable String email, Pageable pageable) {
        try {
            Page<Subscription> subscriptions = subscriptionService.getSubscriptionsByCustomerEmail(email, pageable);
            Page<SubscriptionResponse> responses = subscriptions.map(this::mapToResponse);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting customer subscriptions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting subscriptions: " + e.getMessage()));
        }
    }

    /**
     * Get active subscriptions
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveSubscriptions(Pageable pageable) {
        try {
            Page<Subscription> subscriptions = subscriptionService.getActiveSubscriptions(pageable);
            Page<SubscriptionResponse> responses = subscriptions.map(this::mapToResponse);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting active subscriptions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting subscriptions: " + e.getMessage()));
        }
    }

    /**
     * Search subscriptions
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchSubscriptions(@RequestParam String query, Pageable pageable) {
        try {
            Page<Subscription> subscriptions = subscriptionService.searchSubscriptions(query, pageable);
            Page<SubscriptionResponse> responses = subscriptions.map(this::mapToResponse);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error searching subscriptions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error searching subscriptions: " + e.getMessage()));
        }
    }

    /**
     * Cancel a subscription
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelSubscription(@PathVariable Long id, @RequestBody CancelSubscriptionRequest request) {
        try {
            Subscription subscription = subscriptionService.cancelSubscription(id, request.reason());
            return ResponseEntity.ok(mapToResponse(subscription));
        } catch (IllegalArgumentException e) {
            log.warn("Subscription cancellation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error canceling subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error canceling subscription: " + e.getMessage()));
        }
    }

    /**
     * Cancel subscription by Square ID
     */
    @PostMapping("/square/{squareId}/cancel")
    public ResponseEntity<?> cancelSubscriptionBySquareId(@PathVariable String squareId, 
                                                          @RequestBody CancelSubscriptionRequest request) {
        try {
            Subscription subscription = subscriptionService.cancelSubscriptionBySquareId(squareId, request.reason());
            return ResponseEntity.ok(mapToResponse(subscription));
        } catch (IllegalArgumentException e) {
            log.warn("Subscription cancellation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error canceling subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error canceling subscription: " + e.getMessage()));
        }
    }

    /**
     * Pause a subscription
     */
    @PostMapping("/{id}/pause")
    public ResponseEntity<?> pauseSubscription(@PathVariable Long id) {
        try {
            Subscription subscription = subscriptionService.pauseSubscription(id);
            return ResponseEntity.ok(mapToResponse(subscription));
        } catch (Exception e) {
            log.error("Error pausing subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error pausing subscription: " + e.getMessage()));
        }
    }

    /**
     * Resume a subscription
     */
    @PostMapping("/{id}/resume")
    public ResponseEntity<?> resumeSubscription(@PathVariable Long id) {
        try {
            Subscription subscription = subscriptionService.resumeSubscription(id);
            return ResponseEntity.ok(mapToResponse(subscription));
        } catch (Exception e) {
            log.error("Error resuming subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error resuming subscription: " + e.getMessage()));
        }
    }

    /**
     * Update subscription amount
     */
    @PutMapping("/{id}/amount")
    public ResponseEntity<?> updateSubscriptionAmount(@PathVariable Long id, 
                                                      @RequestBody UpdateSubscriptionAmountRequest request) {
        try {
            Subscription subscription = subscriptionService.updateSubscriptionAmount(id, request.newAmount());
            return ResponseEntity.ok(mapToResponse(subscription));
        } catch (Exception e) {
            log.error("Error updating subscription amount", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating subscription: " + e.getMessage()));
        }
    }

    /**
     * Get subscription statistics
     */
    @GetMapping("/stats/overview")
    public ResponseEntity<?> getStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            Page<Subscription> all = subscriptionService.getAllSubscriptions(Pageable.ofSize(1));
            long totalCount = all.getTotalElements();
            
            Page<Subscription> active = subscriptionService.getActiveSubscriptions(Pageable.ofSize(1));
            long activeCount = active.getTotalElements();
            
            List<Subscription> canceled = subscriptionService.getSubscriptionsByStatus(SubscriptionStatus.CANCELED);
            long canceledCount = canceled.size();
            
            stats.put("total", totalCount);
            stats.put("active", activeCount);
            stats.put("canceled", canceledCount);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting statistics: " + e.getMessage()));
        }
    }

    /**
     * Sync subscription status from Square
     */
    @PostMapping("/{id}/sync-status")
    public ResponseEntity<?> syncSubscriptionStatus(@PathVariable Long id) {
        try {
            Subscription subscription = subscriptionService.syncSubscriptionStatusFromSquare(id);
            return ResponseEntity.ok(mapToResponse(subscription));
        } catch (IllegalArgumentException e) {
            log.warn("Subscription sync failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error syncing subscription status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error syncing subscription: " + e.getMessage()));
        }
    }

    /**
     * Sync status for all subscriptions (batch operation)
     */
    @PostMapping("/sync-all-status")
    public ResponseEntity<?> syncAllSubscriptionStatus() {
        try {
            Page<Subscription> allSubscriptions = subscriptionService.getAllSubscriptions(Pageable.ofSize(1000));
            int syncedCount = 0;
            int errorCount = 0;

            for (Subscription sub : allSubscriptions.getContent()) {
                try {
                    subscriptionService.syncSubscriptionStatusFromSquare(sub);
                    syncedCount++;
                } catch (Exception e) {
                    log.error("Error syncing subscription {}", sub.getId(), e);
                    errorCount++;
                }
            }

            return ResponseEntity.ok(Map.of(
                "synced", syncedCount,
                "errors", errorCount,
                "total", allSubscriptions.getTotalElements()
            ));
        } catch (Exception e) {
            log.error("Error syncing all subscriptions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error syncing subscriptions: " + e.getMessage()));
        }
    }

    /**
     * Pull all current subscriptions from Square and upsert locally
     */
    @PostMapping("/sync-from-square")
    public ResponseEntity<?> syncFromSquare() {
        try {
            var result = subscriptionService.syncAllSubscriptionsFromSquare();
            if (result.errorMessage != null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                                "error", result.errorMessage,
                                "created", result.created.get(),
                                "updated", result.updated.get(),
                                "failures", result.failures.get()
                        ));
            }
            return ResponseEntity.ok(Map.of(
                    "created", result.created.get(),
                    "updated", result.updated.get(),
                    "failures", result.failures.get()
            ));
        } catch (Exception e) {
            log.error("Error syncing subscriptions from Square", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error syncing subscriptions: " + e.getMessage()));
        }
    }

    /**
     * Create a new customer in Square
     * Accepts customer details and delegates to SubscriptionService to create the customer in Square
     * 
     * @param request Map containing: email (required), givenName, familyName, phoneNumber, address (all optional)
     * @return ResponseEntity with created customer details including id, email, givenName, familyName
     */
    @PostMapping("/square/customers")
    public ResponseEntity<?> createSquareCustomer(@RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            String givenName = (String) request.get("givenName");
            String familyName = (String) request.get("familyName");
            String phoneNumber = (String) request.get("phoneNumber");
            String address = (String) request.get("address");
            
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }
            
            Map<String, Object> customer = subscriptionService.createCustomerInSquare(
                email, givenName, familyName, phoneNumber, address);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(customer);
        } catch (Exception e) {
            log.error("Error creating customer in Square", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creating customer: " + e.getMessage()));
        }
    }

    private SubscriptionResponse mapToResponse(Subscription subscription) {
        return new SubscriptionResponse(
            subscription.getId(),
            subscription.getSquareSubscriptionId(),
            subscription.getSquareCustomerId(),
            subscription.getCustomerEmail(),
            subscription.getCustomerName(),
            subscription.getAmount(),
            subscription.getAmountPaid(),
            subscription.getStatus(),
            subscription.getCurrency(),
            subscription.getDescription(),
            subscription.getBillingCycle(),
            subscription.getCreatedAt(),
            subscription.getUpdatedAt(),
            subscription.getCanceledAt(),
            subscription.getCancellationReason(),
            subscription.getAmountPerPayment(),
            subscription.getFrequency(),
            subscription.getEstimatedEndDate(),
            subscription.getNumberOfPayments()
        );
    }
}
