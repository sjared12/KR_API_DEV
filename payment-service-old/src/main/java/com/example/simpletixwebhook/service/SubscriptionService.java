package com.example.simpletixwebhook.service;

import com.example.simpletixwebhook.model.Subscription;
import com.example.simpletixwebhook.model.Subscription.SubscriptionStatus;
import com.example.simpletixwebhook.repository.SubscriptionRepository;
import com.example.simpletixwebhook.repository.SubscriptionInvoiceRepository;
import com.example.simpletixwebhook.model.SubscriptionInvoice;
import com.example.simpletixwebhook.model.SubscriptionInvoice.InvoiceStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for subscription management including creation, cancellation, and status tracking.
 */
@Service
@Transactional
public class SubscriptionService {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionInvoiceRepository subscriptionInvoiceRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SquareInvoiceService squareInvoiceService;

    @Value("${square.api.token}")
    private String squareApiToken;

    private final String SQUARE_API_BASE = "https://connect.squareup.com/v2/subscriptions/";
    private final String SQUARE_API_BASE_SANDBOX = "https://connect.squareupsandbox.com/v2/subscriptions/";

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               SubscriptionInvoiceRepository subscriptionInvoiceRepository,
                               SquareInvoiceService squareInvoiceService) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionInvoiceRepository = subscriptionInvoiceRepository;
        this.squareInvoiceService = squareInvoiceService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Create a new subscription
     */
    public Subscription createSubscription(String squareSubscriptionId, String squareCustomerId, 
                                          String squarePlanId, String customerEmail, BigDecimal amount) {
        // Check if subscription already exists
        if (subscriptionRepository.findBySquareSubscriptionId(squareSubscriptionId).isPresent()) {
            throw new IllegalArgumentException("Subscription with ID " + squareSubscriptionId + " already exists");
        }

        Subscription subscription = new Subscription(squareSubscriptionId, squareCustomerId, 
                                                    squarePlanId, customerEmail, amount);
        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription created: {} for customer {}", squareSubscriptionId, squareCustomerId);
        return saved;
    }

    /**
     * Get subscription by ID
     */
    public Optional<Subscription> getSubscriptionById(Long id) {
        return subscriptionRepository.findById(id);
    }

    /**
     * Get subscription by Square subscription ID
     */
    public Optional<Subscription> getSubscriptionBySquareId(String squareSubscriptionId) {
        return subscriptionRepository.findBySquareSubscriptionId(squareSubscriptionId);
    }

    /**
     * Get all subscriptions for a customer
     */
    public List<Subscription> getCustomerSubscriptions(String squareCustomerId) {
        return subscriptionRepository.findBySquareCustomerId(squareCustomerId);
    }

    /**
     * Get subscriptions by status
     */
    public List<Subscription> getSubscriptionsByStatus(SubscriptionStatus status) {
        return subscriptionRepository.findByStatus(status);
    }

    /**
     * Get all subscriptions with pagination
     */
    public Page<Subscription> getAllSubscriptions(Pageable pageable) {
        return subscriptionRepository.findAll(pageable);
    }

    /**
     * Get active subscriptions with pagination
     */
    public Page<Subscription> getActiveSubscriptions(Pageable pageable) {
        return subscriptionRepository.findActiveSubscriptions(pageable);
    }

    /**
     * Get subscriptions by customer email with pagination
     */
    public Page<Subscription> getSubscriptionsByCustomerEmail(String email, Pageable pageable) {
        return subscriptionRepository.findByCustomerEmail(email, pageable);
    }

    /**
     * Search subscriptions
     */
    public Page<Subscription> searchSubscriptions(String query, Pageable pageable) {
        return subscriptionRepository.searchSubscriptions(query, pageable);
    }

    /**
     * Update subscription status
     */
    public Subscription updateSubscriptionStatus(Long subscriptionId, SubscriptionStatus newStatus) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        SubscriptionStatus oldStatus = subscription.getStatus();
        subscription.setStatus(newStatus);
        Subscription updated = subscriptionRepository.save(subscription);
        log.info("Subscription {} status changed from {} to {}", subscriptionId, oldStatus, newStatus);
        return updated;
    }

    /**
     * Record a payment on subscription and auto-cancel when total is reached.
     * This tracks payments app-side and cancels the Square subscription when paid in full.
     */
    public Subscription recordPayment(Long subscriptionId, BigDecimal paymentAmount) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        BigDecimal newAmountPaid = subscription.getAmountPaid().add(paymentAmount);
        subscription.setAmountPaid(newAmountPaid);

        // Auto-cancel subscription when total amount is reached or exceeded
        BigDecimal remaining = subscription.getAmount().subtract(newAmountPaid);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0 && subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            // Cancel in Square first
            if (subscription.getSquareSubscriptionId() != null && !subscription.getSquareSubscriptionId().isEmpty()) {
                try {
                    cancelSubscriptionInSquare(subscription.getSquareSubscriptionId());
                    log.info("Auto-canceled Square subscription {} after reaching total amount", subscription.getSquareSubscriptionId());
                } catch (Exception e) {
                    log.error("Failed to cancel Square subscription {}, but marking as complete locally", subscription.getSquareSubscriptionId(), e);
                }
            }
            
            subscription.setStatus(SubscriptionStatus.CANCELED);
            subscription.setCancellationReason("Payment plan completed - total amount paid");
            subscription.setCanceledAt(LocalDateTime.now());
            log.info("Subscription {} auto-completed: ${} paid of ${} owed", 
                subscriptionId, newAmountPaid, subscription.getAmount());
        }

        Subscription updated = subscriptionRepository.save(subscription);
        log.info("Payment of {} recorded on subscription {}. Total paid: {}/{}", 
            paymentAmount, subscriptionId, newAmountPaid, subscription.getAmount());
        return updated;
    }

    /**
     * Create an installment invoice (or invoice plan) in Square for a subscription and record it locally.
     * This method uses `SquareInvoiceService` to create the invoice and stores a `SubscriptionInvoice`.
     */
    public SubscriptionInvoice createInvoiceForSubscription(Long subscriptionId, java.math.BigDecimal installmentAmount, SquareInvoiceService.Cadence cadence) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        try {
            SquareInvoiceService.InvoiceResult result = squareInvoiceService.createInstallmentInvoice(
                    subscription.getId().toString(),
                    subscription.getCustomerEmail(),
                    subscription.getAmount(),
                    installmentAmount,
                    cadence
            );

            SubscriptionInvoice inv = new SubscriptionInvoice(subscription, installmentAmount, java.time.LocalDateTime.now());
            inv.setSquareInvoiceId(result.invoiceId());
            inv.setPublicUrl(result.publicUrl());
            inv.setStatus(InvoiceStatus.SENT);
            subscriptionInvoiceRepository.save(inv);
            log.info("Created invoice {} for subscription {}", result.invoiceId(), subscriptionId);
            return inv;
        } catch (Exception e) {
            log.error("Failed to create invoice for subscription {}", subscriptionId, e);
            throw new RuntimeException("Failed to create invoice: " + e.getMessage(), e);
        }
    }

    /**
     * Mark a subscription invoice as paid and update the parent subscription's paid total.
     * Automatically cancels the Square subscription when the total amount is reached.
     */
    public Subscription markInvoicePaid(Long invoiceId, BigDecimal amountPaid) {
        SubscriptionInvoice inv = subscriptionInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (inv.getStatus() == InvoiceStatus.PAID) {
            log.debug("Invoice {} already marked as paid", invoiceId);
            return inv.getSubscription();
        }

        inv.setStatus(InvoiceStatus.PAID);
        inv.setPaidAt(LocalDateTime.now());
        subscriptionInvoiceRepository.save(inv);
        
        log.info("Invoice {} marked as paid with amount {}", invoiceId, amountPaid);

        // Update subscription and check if total is reached
        return recordPayment(inv.getSubscription().getId(), amountPaid);
    }

    /**
     * Update subscription amount
     */
    public Subscription updateSubscriptionAmount(Long subscriptionId, BigDecimal newAmount) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        subscription.setAmount(newAmount);
        Subscription updated = subscriptionRepository.save(subscription);
        log.info("Subscription {} amount updated to {}", subscriptionId, newAmount);
        return updated;
    }


    /**
     * Get subscription by customer email
     */
    public List<Subscription> getSubscriptionsByCustomerEmail(String email) {
        return subscriptionRepository.findByCustomerEmail(email);
    }

    /**
     * Check all active subscriptions and auto-cancel those that have reached their total amount.
     * This is useful for periodic cleanup or webhook processing.
     * @return number of subscriptions auto-canceled
     */
    public int checkAndCompleteSubscriptions() {
        List<Subscription> activeSubscriptions = getSubscriptionsByStatus(SubscriptionStatus.ACTIVE);
        int completedCount = 0;
        
        for (Subscription sub : activeSubscriptions) {
            BigDecimal remaining = sub.getAmount().subtract(sub.getAmountPaid());
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                // Cancel in Square
                if (sub.getSquareSubscriptionId() != null && !sub.getSquareSubscriptionId().isEmpty()) {
                    try {
                        cancelSubscriptionInSquare(sub.getSquareSubscriptionId());
                        log.info("Auto-canceled Square subscription {} during cleanup", sub.getSquareSubscriptionId());
                    } catch (Exception e) {
                        log.error("Failed to cancel Square subscription {} during cleanup", sub.getSquareSubscriptionId(), e);
                    }
                }
                
                sub.setStatus(SubscriptionStatus.CANCELED);
                sub.setCancellationReason("Payment plan completed - total amount paid");
                sub.setCanceledAt(LocalDateTime.now());
                subscriptionRepository.save(sub);
                completedCount++;
                
                log.info("Auto-completed subscription {} during cleanup: ${} paid of ${} owed", 
                    sub.getId(), sub.getAmountPaid(), sub.getAmount());
            }
        }
        
        log.info("Completed {} subscriptions during cleanup check", completedCount);
        return completedCount;
    }

    /**
     * Sync subscription status from Square API
     */
    public Subscription syncSubscriptionStatusFromSquare(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        return syncSubscriptionStatusFromSquare(subscription);
    }

    /**
     * Sync subscription status from Square API
     */
    public Subscription syncSubscriptionStatusFromSquare(Subscription subscription) {
        try {
            String squareSubId = subscription.getSquareSubscriptionId();
            JsonNode squareSubscription = fetchSubscriptionFromSquare(squareSubId);

            if (squareSubscription != null) {
                String status = squareSubscription.has("subscription") 
                    ? squareSubscription.get("subscription").get("status").asText()
                    : squareSubscription.get("status").asText();

                // Map Square subscription status to local status
                SubscriptionStatus localStatus = mapSquareStatusToLocal(status);
                subscription.setStatus(localStatus);
                
                log.info("Subscription {} synced from Square: Square status={}, Local status={}", 
                    squareSubId, status, localStatus);
                
                return subscriptionRepository.save(subscription);
            } else {
                log.warn("Failed to fetch subscription {} from Square API", squareSubId);
                return subscription;
            }
        } catch (Exception e) {
            log.error("Error syncing subscription status from Square", e);
            return subscription;
        }
    }

    /**
     * Fetch subscription details from Square API
     */
    private JsonNode fetchSubscriptionFromSquare(String squareSubscriptionId) {
        try {
            if (squareApiToken == null || squareApiToken.trim().isEmpty()) {
                log.error("Square API token not configured");
                return null;
            }

            String endpoint;
            if (squareApiToken.startsWith("EAA")) {
                endpoint = SQUARE_API_BASE_SANDBOX + squareSubscriptionId;
            } else {
                endpoint = SQUARE_API_BASE + squareSubscriptionId;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + squareApiToken);
            headers.set("Accept", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                endpoint,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readTree(response.getBody());
            } else {
                log.error("Square API error: Status={}, Body={}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Error fetching subscription from Square API for ID: {}", squareSubscriptionId, e);
        }
        return null;
    }

    private JsonNode fetchCustomerFromSquare(String squareCustomerId) {
        try {
            if (squareApiToken == null || squareApiToken.trim().isEmpty()) {
                log.error("Square API token not configured");
                return null;
            }

            String endpoint;
            if (squareApiToken.startsWith("EAA")) {
                endpoint = "https://connect.squareupsandbox.com/v2/customers/" + squareCustomerId;
            } else {
                endpoint = "https://connect.squareup.com/v2/customers/" + squareCustomerId;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + squareApiToken);
            headers.set("Accept", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                endpoint,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readTree(response.getBody());
            } else {
                log.debug("Square customer fetch not found or error: Status={}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.debug("Error fetching customer from Square API for ID: {}", squareCustomerId, e);
        }
        return null;
    }

    /**
     * Create a new customer in Square
     * Accepts customer details and creates a new customer record in Square's system
     * 
     * @param email Customer email address (required)
     * @param givenName Customer first name (optional)
     * @param familyName Customer last name (optional)
     * @param phoneNumber Customer phone number (optional)
     * @param address Customer address (optional)
     * @return Map containing customer id, email, givenName, and familyName
     * @throws Exception if customer creation fails or Square API token not configured
     */
    public Map<String, Object> createCustomerInSquare(String email, String givenName, String familyName, 
                                                      String phoneNumber, String address) throws Exception {
        try {
            if (squareApiToken == null || squareApiToken.trim().isEmpty()) {
                throw new IllegalStateException("Square API token not configured");
            }

            String endpoint;
            if (squareApiToken.startsWith("EAA")) {
                endpoint = "https://connect.squareupsandbox.com/v2/customers";
            } else {
                endpoint = "https://connect.squareup.com/v2/customers";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + squareApiToken);
            headers.set("Accept", "application/json");
            headers.set("Content-Type", "application/json");

            // Build customer request body with provided details
            ObjectNode customerBody = objectMapper.createObjectNode();
            customerBody.put("email_address", email);
            
            if (givenName != null && !givenName.isEmpty()) {
                customerBody.put("given_name", givenName);
            }
            if (familyName != null && !familyName.isEmpty()) {
                customerBody.put("family_name", familyName);
            }
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                customerBody.put("phone_number", phoneNumber);
            }
            if (address != null && !address.isEmpty()) {
                ObjectNode addressNode = objectMapper.createObjectNode();
                addressNode.put("address_line_1", address);
                customerBody.set("address", addressNode);
            }

            HttpEntity<String> entity = new HttpEntity<>(customerBody.toString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode responseNode = objectMapper.readTree(response.getBody());
                JsonNode customer = responseNode.get("customer");
                if (customer != null) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", customer.get("id").asText());
                    result.put("email", customer.get("email_address").asText());
                    if (customer.has("given_name")) {
                        result.put("givenName", customer.get("given_name").asText());
                    }
                    if (customer.has("family_name")) {
                        result.put("familyName", customer.get("family_name").asText());
                    }
                    log.info("Customer created in Square: {}", result.get("id"));
                    return result;
                }
            }
            
            log.error("Failed to create customer in Square: Status={}, Body={}", response.getStatusCode(), response.getBody());
            throw new Exception("Failed to create customer in Square: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("Error creating customer in Square", e);
            throw e;
        }
    }

    /**
     * Pull all current subscriptions from Square and upsert into local DB.
     */
    public SyncResult syncAllSubscriptionsFromSquare() {
        SyncResult result = new SyncResult();

        try {
            if (squareApiToken == null || squareApiToken.trim().isEmpty()) {
                result.errorMessage = "Square API token not configured";
                return result;
            }

            String endpoint;
            if (squareApiToken.startsWith("EAA")) {
                endpoint = "https://connect.squareupsandbox.com/v2/subscriptions/search";
            } else {
                endpoint = "https://connect.squareup.com/v2/subscriptions/search";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + squareApiToken);
            headers.set("Accept", "application/json");
            headers.set("Content-Type", "application/json");

            String cursor = null;
            do {
                ObjectNode body = objectMapper.createObjectNode();
                if (cursor != null) {
                    body.put("cursor", cursor);
                }

                HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
                ResponseEntity<String> response = restTemplate.exchange(
                        endpoint,
                        HttpMethod.POST,
                        entity,
                        String.class
                );

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    log.error("Square search subscriptions failed: status={} body={}", response.getStatusCode(), response.getBody());
                    result.errorMessage = "Square search subscriptions failed";
                    break;
                }

                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode subs = root.get("subscriptions");
                if (subs != null && subs.isArray()) {
                    subs.forEach(node -> {
                        try {
                            upsertFromSquareNode(node, result);
                        } catch (Exception ex) {
                            log.error("Failed to upsert subscription from Square", ex);
                            result.failures.incrementAndGet();
                        }
                    });
                }

                cursor = root.has("cursor") && !root.get("cursor").isNull() ? root.get("cursor").asText() : null;
            } while (cursor != null && !cursor.isBlank());

        } catch (Exception e) {
            log.error("Error syncing all subscriptions from Square", e);
            result.errorMessage = e.getMessage();
        }

        return result;
    }

    private void upsertFromSquareNode(JsonNode node, SyncResult result) {
        if (node == null || !node.has("id")) {
            result.failures.incrementAndGet();
            return;
        }

        String squareId = node.get("id").asText();
        Optional<Subscription> existingOpt = subscriptionRepository.findBySquareSubscriptionId(squareId);

        boolean isNew = existingOpt.isEmpty();
        Subscription sub = existingOpt.orElseGet(Subscription::new);

        sub.setSquareSubscriptionId(squareId);
        String customerId = null;
        if (node.has("customer_id")) {
            customerId = node.get("customer_id").asText();
            sub.setSquareCustomerId(customerId);
        }
        if (node.has("plan_id") && !node.get("plan_id").isNull()) {
            sub.setSquarePlanId(node.get("plan_id").asText());
        } else if (sub.getSquarePlanId() == null) {
            sub.setSquarePlanId("UNKNOWN_PLAN");
        }

        if (node.has("customer_email") && !node.get("customer_email").isNull()) {
            sub.setCustomerEmail(node.get("customer_email").asText());
        } else if (sub.getCustomerEmail() == null) {
            sub.setCustomerEmail("unknown@example.com");
        }

        // Fetch customer name from Square if customer_id is available
        if (customerId != null && !customerId.trim().isEmpty()) {
            JsonNode customerNode = fetchCustomerFromSquare(customerId);
            if (customerNode != null && customerNode.has("customer")) {
                JsonNode customer = customerNode.get("customer");
                String customerName = extractCustomerName(customer);
                if (customerName != null && !customerName.trim().isEmpty()) {
                    sub.setCustomerName(customerName);
                }
            }
        }

        // Monetary fields
        if (node.has("price_override_money") && node.get("price_override_money").has("amount")) {
            BigDecimal amt = new BigDecimal(node.get("price_override_money").get("amount").asLong()).movePointLeft(2);
            sub.setAmount(amt);
        } else if (sub.getAmount() == null) {
            sub.setAmount(BigDecimal.ZERO);
        }
        // status
        if (node.has("status")) {
            SubscriptionStatus localStatus = mapSquareStatusToLocal(node.get("status").asText());
            sub.setStatus(localStatus);
        }
        // dates
        if (isNew && node.has("created_at") && !node.get("created_at").isNull()) {
            sub.setCreatedAt(LocalDateTime.parse(node.get("created_at").asText().replace("Z", "")));
        }
        if (node.has("charged_through_date") && !node.get("charged_through_date").isNull()) {
            sub.setEstimatedEndDate(LocalDateTime.parse(node.get("charged_through_date").asText() + "T00:00:00"));
        }

        subscriptionRepository.save(sub);
        if (isNew) {
            result.created.incrementAndGet();
        } else {
            result.updated.incrementAndGet();
        }
    }

    private String extractCustomerName(JsonNode customer) {
        if (customer == null) return null;

        // Try given_name and family_name first
        StringBuilder name = new StringBuilder();
        if (customer.has("given_name") && !customer.get("given_name").isNull()) {
            name.append(customer.get("given_name").asText());
        }
        if (customer.has("family_name") && !customer.get("family_name").isNull()) {
            if (name.length() > 0) name.append(" ");
            name.append(customer.get("family_name").asText());
        }

        // Fall back to nickname if name is empty
        if (name.length() == 0 && customer.has("nickname") && !customer.get("nickname").isNull()) {
            name.append(customer.get("nickname").asText());
        }

        return name.length() > 0 ? name.toString() : null;
    }

    public static class SyncResult {
        public final AtomicInteger created = new AtomicInteger(0);
        public final AtomicInteger updated = new AtomicInteger(0);
        public final AtomicInteger failures = new AtomicInteger(0);
        public String errorMessage;
    }

    /**
     * Map Square subscription status to local status enum
     */
    private SubscriptionStatus mapSquareStatusToLocal(String squareStatus) {
        if (squareStatus == null) {
            return SubscriptionStatus.PENDING;
        }

        switch (squareStatus.toUpperCase()) {
            case "ACTIVE":
                return SubscriptionStatus.ACTIVE;
            case "CANCELED":
                return SubscriptionStatus.CANCELED;
            case "PAUSED":
                return SubscriptionStatus.PAUSED;
            case "PENDING":
                return SubscriptionStatus.PENDING;
            default:
                log.warn("Unknown Square subscription status: {}", squareStatus);
                return SubscriptionStatus.PENDING;
        }
    }

    /**
     * Pause a subscription in Square and locally
     */
    public Subscription pauseSubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        // Send pause request to Square
        pauseSubscriptionInSquare(subscription.getSquareSubscriptionId());

        // Update local status
        subscription.setStatus(SubscriptionStatus.PAUSED);
        Subscription updated = subscriptionRepository.save(subscription);
        log.info("Subscription {} paused", subscriptionId);
        return updated;
    }

    /**
     * Resume a subscription in Square and locally
     */
    public Subscription resumeSubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        // Send resume request to Square
        resumeSubscriptionInSquare(subscription.getSquareSubscriptionId());

        // Update local status
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        Subscription updated = subscriptionRepository.save(subscription);
        log.info("Subscription {} resumed", subscriptionId);
        return updated;
    }

    /**
     * Cancel a subscription in Square and locally
     */
    public Subscription cancelSubscription(Long subscriptionId, String reason) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        if (subscription.getStatus() == SubscriptionStatus.CANCELED) {
            throw new IllegalArgumentException("Subscription is already canceled");
        }

        // Send cancel request to Square
        cancelSubscriptionInSquare(subscription.getSquareSubscriptionId());

        // Update local status
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setCancellationReason(reason);
        subscription.setCanceledAt(LocalDateTime.now());

        Subscription updated = subscriptionRepository.save(subscription);
        log.info("Subscription {} canceled. Reason: {}", subscriptionId, reason);
        return updated;
    }

    /**
     * Cancel subscription by Square ID
     */
    public Subscription cancelSubscriptionBySquareId(String squareSubscriptionId, String reason) {
        Subscription subscription = subscriptionRepository.findBySquareSubscriptionId(squareSubscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        return cancelSubscription(subscription.getId(), reason);
    }

    /**
     * Send pause request to Square API
     */
    private void pauseSubscriptionInSquare(String squareSubscriptionId) {
        try {
            if (squareApiToken == null || squareApiToken.trim().isEmpty()) {
                log.error("Square API token not configured");
                return;
            }

            String endpoint;
            if (squareApiToken.startsWith("EAA")) {
                endpoint = SQUARE_API_BASE_SANDBOX + squareSubscriptionId + "/pause";
            } else {
                endpoint = SQUARE_API_BASE + squareSubscriptionId + "/pause";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + squareApiToken);
            headers.set("Accept", "application/json");
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Subscription {} paused in Square", squareSubscriptionId);
            } else {
                log.error("Failed to pause subscription in Square: status={}, body={}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Error pausing subscription in Square API for ID: {}", squareSubscriptionId, e);
        }
    }

    /**
     * Send resume request to Square API
     */
    private void resumeSubscriptionInSquare(String squareSubscriptionId) {
        try {
            if (squareApiToken == null || squareApiToken.trim().isEmpty()) {
                log.error("Square API token not configured");
                return;
            }

            String endpoint;
            if (squareApiToken.startsWith("EAA")) {
                endpoint = SQUARE_API_BASE_SANDBOX + squareSubscriptionId + "/resume";
            } else {
                endpoint = SQUARE_API_BASE + squareSubscriptionId + "/resume";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + squareApiToken);
            headers.set("Accept", "application/json");
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Subscription {} resumed in Square", squareSubscriptionId);
            } else {
                log.error("Failed to resume subscription in Square: status={}, body={}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Error resuming subscription in Square API for ID: {}", squareSubscriptionId, e);
        }
    }

    /**
     * Send cancel request to Square API
     */
    private void cancelSubscriptionInSquare(String squareSubscriptionId) {
        try {
            if (squareApiToken == null || squareApiToken.trim().isEmpty()) {
                log.error("Square API token not configured");
                return;
            }

            String endpoint;
            if (squareApiToken.startsWith("EAA")) {
                endpoint = SQUARE_API_BASE_SANDBOX + squareSubscriptionId + "/cancel";
            } else {
                endpoint = SQUARE_API_BASE + squareSubscriptionId + "/cancel";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + squareApiToken);
            headers.set("Accept", "application/json");
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Subscription {} canceled in Square", squareSubscriptionId);
            } else {
                log.error("Failed to cancel subscription in Square: status={}, body={}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Error canceling subscription in Square API for ID: {}", squareSubscriptionId, e);
        }
    }
}
