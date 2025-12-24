package com.example.simpletixwebhook.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SquareInvoiceService {

    @Value("${square.api.token:}")
    private String apiToken;

    @Value("${square.location.id:}")
    private String locationId;

    private static final String SANDBOX_BASE = "https://connect.squareupsandbox.com";
    private static final String PROD_BASE = "https://connect.squareup.com";

    private String apiBase() {
        return (apiToken != null && apiToken.startsWith("EAA")) ? SANDBOX_BASE : PROD_BASE;
    }

    public record InvoiceResult(String invoiceId, String publicUrl) {}

    public InvoiceResult createInstallmentInvoice(String studentId,
                                                  String email,
                                                  BigDecimal totalDue,
                                                  BigDecimal installmentAmount,
                                                  Cadence cadence) throws Exception {
        // Step 1: Create or retrieve customer
        String customerId = createOrGetCustomer(email, studentId);
        
        // Step 2: Create an order with line items
        String orderId = createOrder(studentId, totalDue);
        
        // Step 3: Compute installments
        int steps = totalDue.divide(installmentAmount, 0, java.math.RoundingMode.UP).intValue();
        if (steps < 1) steps = 1;
        if (steps > 24) steps = 24; // safety cap

        StringBuilder paymentRequests = new StringBuilder();
        BigDecimal remaining = totalDue;
        LocalDate dueDate = LocalDate.now();
        LocalDate lastDueDate = dueDate;
        for (int i = 0; i < steps; i++) {
            dueDate = switch (cadence) {
                case WEEKLY -> dueDate.plusWeeks(1);
                case BIWEEKLY -> dueDate.plusWeeks(2);
                case MONTHLY -> dueDate.plusMonths(1);
            };
            BigDecimal thisAmt = (i == steps - 1) ? remaining : installmentAmount.min(remaining);
            long cents = thisAmt.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValue();
            if (i > 0) paymentRequests.append(',');
            paymentRequests.append("{\"request_type\":\"INSTALLMENT\",")
                    .append("\"due_date\":\"").append(dueDate).append("\",")
                    .append("\"tipping_enabled\":false,")
                    .append("\"automatic_payment_source\":\"NONE\",")
                    .append("\"fixed_amount_requested_money\":{\"amount\":").append(cents).append(",\"currency\":\"USD\"}}");
            remaining = remaining.subtract(thisAmt);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            lastDueDate = dueDate;
        }

        String body = "{" +
                "\"invoice\": {" +
                "\"location_id\": \"" + locationId + "\"," +
                "\"order_id\": \"" + orderId + "\"," +
                "\"primary_recipient\": { \"customer_id\": \"" + customerId + "\" }," +
                "\"payment_requests\": [" + paymentRequests + "]," +
                "\"accepted_payment_methods\": {" +
                "  \"card\": true," +
                "  \"square_gift_card\": false," +
                "  \"bank_account\": false," +
                "  \"buy_now_pay_later\": false," +
                "  \"cash_app_pay\": false" +
                "}," +
                "\"delivery_method\": \"EMAIL\"," +
                "\"invoice_number\": \"PLAN-" + studentId + "-" + System.currentTimeMillis() + "\"," +
                "\"title\": \"Payment Plan - Student " + studentId + "\"," +
                "\"description\": \"Installment payment plan\"" +
                " }" +
                "}";
        String createUrl = apiBase() + "/v2/invoices";
        HttpURLConnection conn = (HttpURLConnection) URI.create(createUrl).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiToken);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Square-Version", "2025-10-16");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        if (code != 200 && code != 201) {
            InputStream es = conn.getErrorStream();
            String err = es != null ? new String(es.readAllBytes(), StandardCharsets.UTF_8) : "";
            // Fallback: If merchant lacks Installments (Invoices Plus), create a single invoice and final due date
            if (code == 403 && err != null && err.contains("MERCHANT_SUBSCRIPTION_NOT_FOUND")) {
                LocalDate fallbackDue = lastDueDate != null ? lastDueDate : LocalDate.now().plusMonths(1);
                String fallbackBody = "{" +
                        "\"invoice\": {" +
                        "\"location_id\": \"" + locationId + "\"," +
                        "\"order_id\": \"" + orderId + "\"," +
                        "\"primary_recipient\": { \"customer_id\": \"" + customerId + "\" }," +
                        "\"accepted_payment_methods\": {\"card\": true, \"square_gift_card\": false, \"bank_account\": false, \"buy_now_pay_later\": false, \"cash_app_pay\": false}," +
                        "\"payment_requests\": [{\"request_type\":\"BALANCE\",\"due_date\":\"" + fallbackDue + "\",\"tipping_enabled\":false}]," +
                        "\"delivery_method\": \"EMAIL\"," +
                        "\"invoice_number\": \"PLAN-" + studentId + "-" + System.currentTimeMillis() + "\"," +
                        "\"title\": \"Payment Plan - Student " + studentId + "\"," +
                        "\"description\": \"Payment plan invoice\"" +
                        " }" +
                        "}";
                HttpURLConnection fb = (HttpURLConnection) URI.create(createUrl).toURL().openConnection();
                fb.setRequestMethod("POST");
                fb.setRequestProperty("Authorization", "Bearer " + apiToken);
                fb.setRequestProperty("Content-Type", "application/json");
                fb.setRequestProperty("Square-Version", "2025-10-16");
                fb.setDoOutput(true);
                try (OutputStream os = fb.getOutputStream()) {
                    os.write(fallbackBody.getBytes(StandardCharsets.UTF_8));
                }
                int fbCode = fb.getResponseCode();
                if (fbCode != 200 && fbCode != 201) {
                    InputStream fes = fb.getErrorStream();
                    String ferr = fes != null ? new String(fes.readAllBytes(), StandardCharsets.UTF_8) : "";
                    throw new RuntimeException("Invoice create failed (fallback): " + fbCode + " - " + ferr);
                }
                String fbResp;
                try (InputStream is = fb.getInputStream()) {
                    fbResp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
                String fbInvoiceId = jsonGet(fbResp, "invoice", "id");
                if (fbInvoiceId == null || fbInvoiceId.isBlank()) {
                    fbInvoiceId = extract(fbResp, "\"id\":\"");
                }
                String publishUrl = apiBase() + "/v2/invoices/" + fbInvoiceId + "/publish";
                HttpURLConnection pub = (HttpURLConnection) URI.create(publishUrl).toURL().openConnection();
                pub.setRequestMethod("POST");
                pub.setRequestProperty("Authorization", "Bearer " + apiToken);
                pub.setRequestProperty("Content-Type", "application/json");
                pub.setRequestProperty("Square-Version", "2025-10-16");
                pub.setDoOutput(true);
                try (OutputStream os = pub.getOutputStream()) { os.write("{}".getBytes(StandardCharsets.UTF_8)); }
                int pCode = pub.getResponseCode();
                if (pCode != 200 && pCode != 201) {
                    InputStream pes = pub.getErrorStream();
                    String perr = pes != null ? new String(pes.readAllBytes(), StandardCharsets.UTF_8) : "";
                    throw new RuntimeException("Invoice publish failed (fallback): " + pCode + " - " + perr);
                }
                String pResp;
                try (InputStream is = pub.getInputStream()) { pResp = new String(is.readAllBytes(), StandardCharsets.UTF_8); }
                String url = jsonGet(pResp, "invoice", "public_url");
                if (url == null || url.isBlank()) { url = extract(pResp, "\"public_url\":\""); }
                return new InvoiceResult(fbInvoiceId, url);
            }
            throw new RuntimeException("Invoice create failed: " + code + " - " + err);
        }
        String resp;
        try (InputStream is = conn.getInputStream()) {
            resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        String invoiceId = jsonGet(resp, "invoice", "id");
        if (invoiceId == null || invoiceId.isBlank()) {
            invoiceId = extract(resp, "\"id\":\"");
        }

        String publishUrl = apiBase() + "/v2/invoices/" + invoiceId + "/publish";
        HttpURLConnection pub = (HttpURLConnection) URI.create(publishUrl).toURL().openConnection();
        pub.setRequestMethod("POST");
        pub.setRequestProperty("Authorization", "Bearer " + apiToken);
        pub.setRequestProperty("Content-Type", "application/json");
        pub.setRequestProperty("Square-Version", "2025-10-16");
        pub.setDoOutput(true);
        try (OutputStream os = pub.getOutputStream()) {
            os.write("{}".getBytes(StandardCharsets.UTF_8));
        }
        int pCode = pub.getResponseCode();
        if (pCode != 200 && pCode != 201) {
            InputStream es = pub.getErrorStream();
            String err = es != null ? new String(es.readAllBytes(), StandardCharsets.UTF_8) : "";
            throw new RuntimeException("Invoice publish failed: " + pCode + " - " + err);
        }
        String pResp;
        try (InputStream is = pub.getInputStream()) {
            pResp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        String url = jsonGet(pResp, "invoice", "public_url");
        if (url == null || url.isBlank()) {
            url = extract(pResp, "\"public_url\":\"");
        }
        return new InvoiceResult(invoiceId, url);
    }

    public enum Cadence { WEEKLY, BIWEEKLY, MONTHLY }

    private String createOrGetCustomer(String email, String studentId) throws Exception {
        // Search for existing customer by email
        String searchUrl = apiBase() + "/v2/customers/search";
        String searchBody = "{\"query\":{\"filter\":{\"email_address\":{\"exact\":\"" + email + "\"}}}}";
        HttpURLConnection conn = (HttpURLConnection) URI.create(searchUrl).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiToken);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Square-Version", "2025-10-16");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(searchBody.getBytes(StandardCharsets.UTF_8));
        }
        
        int code = conn.getResponseCode();
        String resp;
        if (code == 200) {
            try (InputStream is = conn.getInputStream()) {
                resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            // Try extracting from customers array first (search response wraps in array)
            String existingId = jsonGet(resp, "customers", "id");
            if (existingId == null || existingId.isEmpty()) {
                // Fallback to direct id
                existingId = extract(resp, "\"id\":\"");
            }
            if (existingId != null && !existingId.isEmpty()) {
                return existingId;
            }
        }
        
        // Create new customer
        String createUrl = apiBase() + "/v2/customers";
        String createBody = "{\"email_address\":\"" + email + "\"," +
                "\"reference_id\":\"student-" + studentId + "\"," +
                "\"note\":\"Student ID: " + studentId + "\"}";
        HttpURLConnection createConn = (HttpURLConnection) URI.create(createUrl).toURL().openConnection();
        createConn.setRequestMethod("POST");
        createConn.setRequestProperty("Authorization", "Bearer " + apiToken);
        createConn.setRequestProperty("Content-Type", "application/json");
        createConn.setRequestProperty("Square-Version", "2025-10-16");
        createConn.setDoOutput(true);
        try (OutputStream os = createConn.getOutputStream()) {
            os.write(createBody.getBytes(StandardCharsets.UTF_8));
        }
        
        int createCode = createConn.getResponseCode();
        if (createCode != 200 && createCode != 201) {
            InputStream es = createConn.getErrorStream();
            String err = es != null ? new String(es.readAllBytes(), StandardCharsets.UTF_8) : "";
            throw new RuntimeException("Customer create failed: " + createCode + " - " + err);
        }
        
        try (InputStream is = createConn.getInputStream()) {
            resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        // Try extracting from customer object wrapper first
        String customerId = jsonGet(resp, "customer", "id");
        if (customerId == null || customerId.isEmpty()) {
            // Fallback to direct id
            customerId = extract(resp, "\"id\":\"");
        }
        if (customerId == null || customerId.isEmpty()) {
            throw new RuntimeException("Failed to extract customer ID from response: " + resp);
        }
        return customerId;
    }
    
    private String createOrder(String studentId, BigDecimal totalDue) throws Exception {
        long cents = totalDue.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValue();
        String orderBody = "{\"order\":{" +
                "\"location_id\":\"" + locationId + "\"," +
                "\"reference_id\":\"plan-" + studentId + "\"," +
                "\"line_items\":[{" +
                "\"name\":\"Payment Plan - Student " + studentId + "\"," +
                "\"quantity\":\"1\"," +
                "\"base_price_money\":{\"amount\":" + cents + ",\"currency\":\"USD\"}" +
                "}]}}";
        
        String createUrl = apiBase() + "/v2/orders";
        HttpURLConnection conn = (HttpURLConnection) URI.create(createUrl).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiToken);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Square-Version", "2025-10-16");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(orderBody.getBytes(StandardCharsets.UTF_8));
        }
        
        int code = conn.getResponseCode();
        if (code != 200 && code != 201) {
            InputStream es = conn.getErrorStream();
            String err = es != null ? new String(es.readAllBytes(), StandardCharsets.UTF_8) : "";
            throw new RuntimeException("Order create failed: " + code + " - " + err);
        }
        
        String resp;
        try (InputStream is = conn.getInputStream()) {
            resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        // Try extracting from order object wrapper first
        String orderId = jsonGet(resp, "order", "id");
        if (orderId == null || orderId.isEmpty()) {
            // Fallback to direct id
            orderId = extract(resp, "\"id\":\"");
        }
        if (orderId == null || orderId.isEmpty()) {
            throw new RuntimeException("Failed to extract order ID from response: " + resp);
        }
        return orderId;
    }

    private String extract(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int start = idx + key.length();
        int end = json.indexOf('"', start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    // Robust JSON navigation using Jackson to handle whitespace and nesting/arrays
    private String jsonGet(String json, String... path) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);
            for (String p : path) {
                if (node == null) return null;
                // If current node is an array, default to first element
                if (node.isArray()) {
                    if (node.size() == 0) return null;
                    node = node.get(0);
                }
                node = node.get(p);
            }
            if (node == null) return null;
            if (node.isValueNode()) return node.asText();
            return node.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
