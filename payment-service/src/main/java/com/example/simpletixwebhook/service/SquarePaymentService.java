package com.example.simpletixwebhook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Service
public class SquarePaymentService {

    private static final Logger log = LoggerFactory.getLogger(SquarePaymentService.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(55);

    @Value("${square.api.token:}")
    private String apiToken;

    @Value("${square.location.id:}")
    private String locationId;

    @Value("${square.application.id:}")
    private String applicationId;

    private static final String SANDBOX_BASE = "https://connect.squareupsandbox.com";
    private static final String PROD_BASE = "https://connect.squareup.com";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public record PaymentResult(String paymentId, String status, String receiptUrl) {}

    public String getApplicationId() {
        return applicationId;
    }

    public String getLocationId() {
        return locationId;
    }

    private boolean useSandboxEndpoints() {
        if (applicationId != null && applicationId.startsWith("sandbox-")) {
            return true;
        }
        if (apiToken != null && apiToken.startsWith("EAAAE")) {
            return true;
        }
        return false;
    }

    private String paymentsEndpoint() {
        return (useSandboxEndpoints() ? SANDBOX_BASE : PROD_BASE) + "/v2/payments";
    }

    public PaymentResult takePayment(BigDecimal amount,
                                     String studentId,
                                     String sourceId,
                                     String verificationToken,
                                     String email,
                                     String paymentMethod,
                                     String accountHolderName) throws Exception {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("Missing payment sourceId from Web Payments SDK");
        }
        if (apiToken == null || apiToken.isBlank()) {
            throw new IllegalStateException("Square API token is not configured");
        }
        if (locationId == null || locationId.isBlank()) {
            throw new IllegalStateException("Square location ID is not configured");
        }

        long cents;
        try {
            cents = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Amount must have at most two decimal places", ex);
        }
        ObjectNode body = MAPPER.createObjectNode();
        body.put("idempotency_key", UUID.randomUUID().toString());
        ObjectNode amountMoney = body.putObject("amount_money");
        amountMoney.put("amount", cents);
        amountMoney.put("currency", "USD");
        body.put("source_id", sourceId);
        body.put("location_id", locationId);
        body.put("autocomplete", true);
        body.put("reference_id", studentId != null ? studentId : "");
        body.put("note", "Student Payment " + (studentId != null ? studentId : ""));
        if (verificationToken != null && !verificationToken.isBlank()) {
            body.put("verification_token", verificationToken);
        }
        if (email != null && !email.isBlank()) {
            body.put("buyer_email_address", email);
        }
        ObjectNode metadata = body.putObject("metadata");
        if (studentId != null && !studentId.isBlank()) {
            metadata.put("student_id", studentId);
        }
        if (paymentMethod != null && !paymentMethod.isBlank()) {
            metadata.put("payment_method", paymentMethod);
        }
        if (accountHolderName != null && !accountHolderName.isBlank()) {
            metadata.put("account_holder_name", accountHolderName);
        }
        if (!metadata.fieldNames().hasNext()) {
            body.remove("metadata");
        }

        String json = MAPPER.writeValueAsString(body);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(paymentsEndpoint()).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Square-Version", "2025-10-16");
            conn.setDoOutput(true);
            conn.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
            conn.setReadTimeout((int) READ_TIMEOUT.toMillis());

            log.debug("Submitting Square payment for student {} amount {}", studentId, amount);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
            int statusCode = conn.getResponseCode();
            InputStream responseStream = statusCode >= 200 && statusCode < 300
                    ? conn.getInputStream()
                    : conn.getErrorStream();
            String responseBody = responseStream != null
                    ? new String(responseStream.readAllBytes(), StandardCharsets.UTF_8)
                    : "";
            if (statusCode != 200 && statusCode != 201) {
                throw buildSquareException(statusCode, responseBody);
            }

            JsonNode root = MAPPER.readTree(responseBody);
            JsonNode payment = root.path("payment");
            String paymentId = payment.path("id").asText(null);
            String status = payment.path("status").asText(null);
            String receiptUrl = payment.path("receipt_url").asText(null);
            log.info("Square payment {} completed with status {}", paymentId, status);
            return new PaymentResult(paymentId, status, receiptUrl);
        } catch (java.net.SocketTimeoutException timeout) {
            log.error("Square payment request timed out", timeout);
            throw new RuntimeException("Square payment request timed out. Please try again.", timeout);
        } catch (Exception ex) {
            log.error("Unexpected Square payment error", ex);
            throw ex;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private SquarePaymentException buildSquareException(int statusCode, String responseBody) {
        try {
            JsonNode root = responseBody != null && !responseBody.isBlank()
                    ? MAPPER.readTree(responseBody)
                    : null;
            if (root != null && root.has("errors") && root.get("errors").isArray() && root.get("errors").size() > 0) {
                JsonNode first = root.get("errors").get(0);
                String sqCode = first.path("code").asText(null);
                String sqCategory = first.path("category").asText(null);
                String detail = first.path("detail").asText(null);
                String friendly = translateSquareDetail(sqCode, detail);
                log.error("Square payment API error {} code {} category {} detail {}", statusCode, sqCode, sqCategory, detail);
                return new SquarePaymentException(statusCode, sqCode, sqCategory, friendly);
            }
        } catch (Exception parseEx) {
            log.error("Failed to parse Square error response", parseEx);
        }
        log.error("Square payment API error {} body {}", statusCode, responseBody);
        return new SquarePaymentException(statusCode, null, null, "Square payment failed. Please try again.");
    }

    private String translateSquareDetail(String sqCode, String detail) {
        if (sqCode != null && sqCode.equals("NOT_FOUND") && detail != null && detail.contains("Card nonce not found")) {
            return "Your payment session expired. Reload the page and try again.";
        }
        if (detail != null && !detail.isBlank()) {
            return detail;
        }
        return "Square payment failed. Please try again.";
    }
}
