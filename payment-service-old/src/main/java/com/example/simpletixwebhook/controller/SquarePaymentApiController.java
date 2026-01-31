package com.example.simpletixwebhook.controller;

import com.example.simpletixwebhook.service.PaymentPlanService;
import com.example.simpletixwebhook.service.SquarePaymentException;
import com.example.simpletixwebhook.service.SquarePaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class SquarePaymentApiController {

    private static final Logger log = LoggerFactory.getLogger(SquarePaymentApiController.class);

    private final String squareApplicationId;
    private final String squareLocationId;
    private final boolean achEnabled;
    private final SquarePaymentService paymentService;
    private final PaymentPlanService paymentPlanService;

    public record ConfigResponse(String applicationId, String locationId, boolean achEnabled) {}
    public record ChargeRequest(@NotNull BigDecimal amount,
                                @NotBlank String studentId,
                                @NotBlank String sourceId,
                                String verificationToken,
                                @Email String email,
                                String paymentMethod,
                                String accountHolderName) {}
    public record ChargeResponse(String paymentId, String status, String receiptUrl) {}
    public record ErrorResponse(String message) {}

    public SquarePaymentApiController(
            @Value("${square.application.id:}") String squareApplicationId,
            @Value("${square.location.id:}") String squareLocationId,
            @Value("${square.ach.enabled:false}") boolean achEnabled,
            SquarePaymentService paymentService,
            PaymentPlanService paymentPlanService) {
        this.squareApplicationId = squareApplicationId;
        this.squareLocationId = squareLocationId;
        this.achEnabled = achEnabled;
        this.paymentService = paymentService;
        this.paymentPlanService = paymentPlanService;
    }

    @GetMapping("/config")
    public ResponseEntity<?> config() {
        if (squareApplicationId == null || squareApplicationId.isBlank() ||
            squareLocationId == null || squareLocationId.isBlank()) {
            log.error("Square configuration missing: appId='{}', locationId='{}'", squareApplicationId, squareLocationId);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("Square configuration is not available. Please contact support."));
        }
        return ResponseEntity.ok(new ConfigResponse(squareApplicationId, squareLocationId, achEnabled));
    }

    @PostMapping("/charge")
    public ResponseEntity<?> charge(@Valid @RequestBody ChargeRequest request) {
        String method = request.paymentMethod() != null ? request.paymentMethod() : "CARD";
        try {
            var result = paymentService.takePayment(
                    request.amount(),
                    request.studentId(),
                    request.sourceId(),
                    request.verificationToken(),
                    request.email(),
                    method,
                    request.accountHolderName());
            
            // Apply payment to active payment plan if one exists
            boolean appliedToPlan = paymentPlanService.applyPaymentByStudentId(
                    request.studentId(),
                    request.amount(),
                    result.paymentId()
            );
            
            if (appliedToPlan) {
                log.info("Payment {} for ${} applied to payment plan for student {}", 
                        result.paymentId(), request.amount(), request.studentId());
            }
            
            return ResponseEntity.ok(new ChargeResponse(result.paymentId(), result.status(), result.receiptUrl()));
        } catch (SquarePaymentException ex) {
            var status = ex.toHttpStatus();
            log.warn("Square rejected payment ({}) for student {}: {}", method, request.studentId(), ex.getMessage());
            return ResponseEntity.status(status != null ? status : HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid charge request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to process payment ({}) for student {}", method, request.studentId(), ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("Square charge failed: " + ex.getMessage()));
        }
    }
}

