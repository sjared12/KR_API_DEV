package com.example.simpletixwebhook.service;

import com.example.simpletixwebhook.model.PaymentRecord;
import com.example.simpletixwebhook.repository.PaymentRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
// import javax.annotation.PostConstruct; // Removed, not needed
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SquarePaymentSyncService {

    private static final Logger log = LoggerFactory.getLogger(SquarePaymentSyncService.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(55);
    private static final String SANDBOX_BASE = "https://connect.squareupsandbox.com";
    private static final String PROD_BASE = "https://connect.squareup.com";

    private final PaymentRecordRepository paymentRecordRepository;

    @Value("${square.api.token:}")
    private String apiToken;

    @Value("${square.location.id:}")
    private String locationId;

    @Value("${SQUARE_API_ENV:dev}")
    private String apiEnv;

    public SquarePaymentSyncService(PaymentRecordRepository paymentRecordRepository) {
        this.paymentRecordRepository = paymentRecordRepository;
    }

    public record SyncResult(int importedCount) {}

    public SyncResult syncPayments() throws Exception {
        if (!StringUtils.hasText(apiToken)) {
            throw new IllegalStateException("Square API token is not configured");
        }
        if (!StringUtils.hasText(locationId)) {
            throw new IllegalStateException("Square location ID is not configured");
        }

        log.debug("[Square Sync] SQUARE_API_ENV: {}", apiEnv);

        log.debug("[Square Sync] Raw apiToken: {}", apiToken);
        log.debug("[Square Sync] Raw locationId: {}", locationId);
        // Removed useSandboxEndpoints() references; now environment is controlled by SQUARE_API_ENV

        String base = selectApiBase();
        log.info("Square sync using {} base with location {} (token prefix {})",
            base.contains("sandbox") ? "sandbox" : "production",
            locationId,
            redactToken(apiToken));

        Instant begin = paymentRecordRepository.findTopByOrderByCreatedAtDesc()
                .map(PaymentRecord::getCreatedAt)
                .map(ts -> ts.minusSeconds(5)) // overlap slightly to avoid gaps
                .orElse(null);

        AtomicInteger imported = new AtomicInteger();
        String cursor = null;
        do {
            ObjectNode body = MAPPER.createObjectNode();
            ArrayNode locations = body.putArray("location_ids");
            locations.add(locationId);
            body.put("sort_order", "ASC");
            body.put("limit", 100);
            if (begin != null) {
                body.put("begin_time", begin.toString());
            }
            if (cursor != null) {
                body.put("cursor", cursor);
            }

            String json = MAPPER.writeValueAsString(body);
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) URI.create(paymentsSearchEndpoint()).toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + apiToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Square-Version", "2025-10-16");
                conn.setDoOutput(true);
                conn.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
                conn.setReadTimeout((int) READ_TIMEOUT.toMillis());

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int status = conn.getResponseCode();
                InputStream responseStream = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
                String responseBody = responseStream != null ? new String(responseStream.readAllBytes(), StandardCharsets.UTF_8) : "";
                if (status != 200) {
                    log.error("Square payment search failed status {} body {}", status, responseBody);
                    throw new RuntimeException("Square payment search failed with status " + status);
                }

                JsonNode root = MAPPER.readTree(responseBody);
                JsonNode payments = root.path("payments");
                if (payments.isArray()) {
                    payments.forEach(p -> saveIfNew(p, imported));
                }
                cursor = root.path("cursor").asText(null);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        } while (cursor != null && !cursor.isBlank());

        log.info("Square payment sync imported {} payments", imported.get());
        return new SyncResult(imported.get());
    }

    private void saveIfNew(JsonNode payment, AtomicInteger imported) {
        String squareId = payment.path("id").asText(null);
        if (!StringUtils.hasText(squareId)) {
            return;
        }
        if (paymentRecordRepository.existsBySquarePaymentId(squareId)) {
            return;
        }
        JsonNode amountNode = payment.path("amount_money");
        long amountCents = amountNode.path("amount").asLong(0);
        String currency = amountNode.path("currency").asText("USD");
        BigDecimal amount = BigDecimal.valueOf(amountCents).movePointLeft(2);

        PaymentRecord rec = new PaymentRecord();
        rec.setSquarePaymentId(squareId);
        rec.setAmount(amount);
        rec.setCurrency(currency);
        rec.setStatus(payment.path("status").asText("UNKNOWN"));
        rec.setStudentId(payment.path("reference_id").asText(null));
        rec.setReceiptUrl(payment.path("receipt_url").asText(null));
        String created = payment.path("created_at").asText(null);
        if (StringUtils.hasText(created)) {
            try {
                rec.setCreatedAt(Instant.parse(created));
            } catch (Exception ex) {
                log.warn("Could not parse Square payment created_at {}", created, ex);
            }
        }
        String updated = payment.path("updated_at").asText(null);
        if (StringUtils.hasText(updated)) {
            try {
                rec.setUpdatedAt(Instant.parse(updated));
            } catch (Exception ex) {
                log.warn("Could not parse Square payment updated_at {}", updated, ex);
            }
        }

        paymentRecordRepository.save(rec);
        imported.incrementAndGet();
    }


    private String paymentsSearchEndpoint() {
        String base = selectApiBase();
        return base + "/v2/payments/search";
    }

    private String selectApiBase() {
        // If SQUARE_API_ENV is 'prod', use production; otherwise use sandbox
        if ("prod".equalsIgnoreCase(apiEnv)) {
            return PROD_BASE;
        }
        return SANDBOX_BASE;
    }

    private String redactToken(String token) {
        if (!StringUtils.hasText(token)) {
            return "<empty>";
        }
        if (token.length() <= 6) {
            return "****";
        }
        return token.substring(0, 6) + "****";
    }
}
