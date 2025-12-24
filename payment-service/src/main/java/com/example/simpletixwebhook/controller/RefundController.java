package com.example.simpletixwebhook.controller;

import com.example.simpletixwebhook.controller.dto.RefundResponse;
import com.example.simpletixwebhook.controller.dto.RequestRefundRequest;
import com.example.simpletixwebhook.controller.dto.ApproveRefundRequest;
import com.example.simpletixwebhook.controller.dto.RejectRefundRequest;
import com.example.simpletixwebhook.controller.dto.CompleteRefundRequest;
import com.example.simpletixwebhook.model.Subscription;
import com.example.simpletixwebhook.model.SubscriptionRefund;
import com.example.simpletixwebhook.model.SubscriptionRefund.RefundStatus;
import com.example.simpletixwebhook.model.User;
import com.example.simpletixwebhook.service.RefundService;
import com.example.simpletixwebhook.service.SubscriptionService;
import com.example.simpletixwebhook.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

/**
 * REST Controller for subscription refund management with approval workflow
 */
@RestController
@RequestMapping("/api/refunds")
@CrossOrigin(origins = "*")
public class RefundController {
    private static final Logger log = LoggerFactory.getLogger(RefundController.class);

    private final RefundService refundService;
    private final SubscriptionService subscriptionService;
    private final UserService userService;

    public RefundController(RefundService refundService, SubscriptionService subscriptionService, UserService userService) {
        this.refundService = refundService;
        this.subscriptionService = subscriptionService;
        this.userService = userService;
    }

    /**
     * Request a refund for a subscription
     */
    @PostMapping("/request")
    public ResponseEntity<?> requestRefund(@RequestBody RequestRefundRequest request,
                                          @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            Subscription subscription = subscriptionService.getSubscriptionById(request.subscriptionId())
                    .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

            // Get the user making the request (defaults to first admin user if not provided)
            User requestingUser = null;
            if (userId != null) {
                requestingUser = userService.getUserById(userId).orElse(null);
            }

            SubscriptionRefund refund = refundService.requestRefund(
                subscription,
                request.requestedAmount(),
                request.reason(),
                requestingUser
            );

            RefundResponse response = mapToResponse(refund);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Refund request failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error requesting refund", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error requesting refund: " + e.getMessage()));
        }
    }

    /**
     * Get refund by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getRefund(@PathVariable Long id) {
        try {
            Optional<SubscriptionRefund> refund = refundService.getRefundById(id);

            if (refund.isPresent()) {
                return ResponseEntity.ok(mapToResponse(refund.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting refund", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting refund: " + e.getMessage()));
        }
    }

    /**
     * Get all refunds with pagination
     */
    @GetMapping
    public ResponseEntity<?> getAllRefunds(Pageable pageable) {
        try {
            Page<SubscriptionRefund> refunds = refundService.getAllRefunds(pageable);
            Page<RefundResponse> responses = refunds.map(this::mapToResponse);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting refunds", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting refunds: " + e.getMessage()));
        }
    }

    /**
     * Get refunds by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getRefundsByStatus(@PathVariable String status, Pageable pageable) {
        try {
            RefundStatus refundStatus = RefundStatus.valueOf(status.toUpperCase());
            Page<SubscriptionRefund> refunds = refundService.getRefundsByStatus(refundStatus, pageable);
            Page<RefundResponse> responses = refunds.map(this::mapToResponse);
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status));
        } catch (Exception e) {
            log.error("Error getting refunds by status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting refunds: " + e.getMessage()));
        }
    }

    /**
     * Get all refunds pending approval
     */
    @GetMapping("/pending-approvals")
    public ResponseEntity<?> getPendingApprovals(Pageable pageable) {
        try {
            Page<SubscriptionRefund> refunds = refundService.getPendingApprovals(pageable);
            Page<RefundResponse> responses = refunds.map(this::mapToResponse);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting pending approvals", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting pending approvals: " + e.getMessage()));
        }
    }

    /**
     * Get count of refunds pending approval
     */
    @GetMapping("/pending-approvals/count")
    public ResponseEntity<?> countPendingApprovals() {
        try {
            long count = refundService.countPendingApprovals();
            return ResponseEntity.ok(Map.of("pendingCount", count));
        } catch (Exception e) {
            log.error("Error counting pending approvals", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error counting pending approvals: " + e.getMessage()));
        }
    }

    /**
     * Get refunds for a subscription
     */
    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<?> getSubscriptionRefunds(@PathVariable Long subscriptionId, Pageable pageable) {
        try {
            Subscription subscription = subscriptionService.getSubscriptionById(subscriptionId)
                    .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

            Page<SubscriptionRefund> refunds = refundService.getSubscriptionRefunds(subscription, pageable);
            Page<RefundResponse> responses = refunds.map(this::mapToResponse);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting subscription refunds", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting refunds: " + e.getMessage()));
        }
    }

    /**
     * Approve a refund with fee deduction
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveRefund(@PathVariable Long id,
                                          @RequestBody ApproveRefundRequest request,
                                          @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            // Get the user approving the refund
            User approvingUser = null;
            if (userId != null) {
                approvingUser = userService.getUserById(userId).orElse(null);
            }

            BigDecimal feePercent = request.processingFeePercent() != null ? 
                    request.processingFeePercent() : new BigDecimal("2.5");

            SubscriptionRefund refund = refundService.approveRefund(
                id,
                feePercent,
                request.notes(),
                approvingUser
            );

            RefundResponse response = mapToResponse(refund);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Refund approval failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error approving refund", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error approving refund: " + e.getMessage()));
        }
    }

    /**
     * Reject a refund request
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectRefund(@PathVariable Long id,
                                         @RequestBody RejectRefundRequest request,
                                         @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            // Get the user rejecting the refund
            User rejectingUser = null;
            if (userId != null) {
                rejectingUser = userService.getUserById(userId).orElse(null);
            }

            SubscriptionRefund refund = refundService.rejectRefund(
                id,
                request.rejectionReason(),
                rejectingUser
            );

            RefundResponse response = mapToResponse(refund);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Refund rejection failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error rejecting refund", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error rejecting refund: " + e.getMessage()));
        }
    }

    /**
     * Mark refund as processing (ready to send to Square)
     */
    @PostMapping("/{id}/mark-processing")
    public ResponseEntity<?> markAsProcessing(@PathVariable Long id) {
        try {
            SubscriptionRefund refund = refundService.markAsProcessing(id);
            RefundResponse response = mapToResponse(refund);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Error marking refund as processing: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error marking refund as processing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error marking refund as processing: " + e.getMessage()));
        }
    }

    /**
     * Mark refund as completed (after processing in Square)
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeRefund(@PathVariable Long id,
                                           @RequestBody CompleteRefundRequest request) {
        try {
            SubscriptionRefund refund = refundService.completeRefund(
                id,
                request.squareRefundId(),
                request.refundedAmount()
            );

            RefundResponse response = mapToResponse(refund);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Error completing refund: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error completing refund", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error completing refund: " + e.getMessage()));
        }
    }

    /**
     * Get refunds approved but not yet processed in Square
     */
    @GetMapping("/pending-square-processing")
    public ResponseEntity<?> getApprovedRefundsPendingSquareProcessing(Pageable pageable) {
        try {
            Page<SubscriptionRefund> refunds = refundService.getApprovedRefundsPendingSquareProcessing(pageable);
            Page<RefundResponse> responses = refunds.map(this::mapToResponse);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting pending Square processing refunds", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting refunds: " + e.getMessage()));
        }
    }

    /**
     * Get refund statistics
     */
    @GetMapping("/stats/overview")
    public ResponseEntity<?> getStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            long pendingCount = refundService.countPendingApprovals();
            Page<SubscriptionRefund> completed = refundService.getRefundsByStatus(RefundStatus.COMPLETED, Pageable.ofSize(1));
            long completedCount = completed.getTotalElements();
            
            stats.put("pendingApproval", pendingCount);
            stats.put("completed", completedCount);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting statistics: " + e.getMessage()));
        }
    }

    private RefundResponse mapToResponse(SubscriptionRefund refund) {
        return new RefundResponse(
            refund.getId(),
            refund.getSubscription().getId(),
            refund.getSubscription().getSquareSubscriptionId(),
            refund.getRequestedAmount(),
            refund.getProcessingFee(),
            refund.getApprovedAmount(),
            refund.getRefundedAmount(),
            refund.getStatus(),
            refund.getRefundReason(),
            refund.getRejectionReason(),
            refund.getSquareRefundId(),
            refund.getRequestedByUser() != null ? refund.getRequestedByUser().getUsername() : null,
            refund.getApprovedByUser() != null ? refund.getApprovedByUser().getUsername() : null,
            refund.getCreatedAt(),
            refund.getApprovedAt(),
            refund.getCompletedAt(),
            refund.getNotes()
        );
    }
}
