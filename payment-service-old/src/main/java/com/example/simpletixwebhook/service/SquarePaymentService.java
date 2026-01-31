package com.example.simpletixwebhook.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SquarePaymentService {
	public record CustomerCardResult(String customerId, String cardId) {}

	/**
	 * Creates a Square customer and attaches a card using the provided card token.
	 * @param email Customer email
	 * @param cardToken Card nonce/token from Square frontend
	 * @return CustomerCardResult with customerId and cardId
	 */
	public CustomerCardResult createCustomerAndCard(String email, String cardToken) {
		try {
			String customerId = createCustomer(email);
			String cardId = attachCard(customerId, cardToken);
			return new CustomerCardResult(customerId, cardId);
		} catch (Exception ex) {
			throw new SquarePaymentException(500, "SQUARE_API_ERROR", "API", "Failed to create customer/card: " + ex.getMessage());
		}
	}

	@Value("${square.api.token:}")
	private String apiToken;

	@Value("${square.location.id:}")
	private String locationId;

	private static final String SANDBOX_BASE = "https://connect.squareupsandbox.com";
	private static final String PROD_BASE = "https://connect.squareup.com";

	private String apiBase() {
		return (apiToken != null && apiToken.startsWith("EAA")) ? SANDBOX_BASE : PROD_BASE;
	}

	public record PaymentResult(String paymentId, String status, String receiptUrl) {}

	/**
	 * Takes a payment using Square API.
	 * @param amount Payment amount
	 * @param studentId Student ID
	 * @param sourceId Card nonce/token
	 * @param verificationToken Optional verification token
	 * @param email Customer email
	 * @param paymentMethod Payment method (e.g., CARD)
	 * @param accountHolderName Optional account holder name
	 * @return PaymentResult with paymentId, status, and receiptUrl
	 */
	public PaymentResult takePayment(java.math.BigDecimal amount, String studentId, String sourceId, String verificationToken, String email, String paymentMethod, String accountHolderName) {
		try {
			// Step 1: Create or get customer
			String customerId = createCustomer(email);
			// Step 2: Build payment request
			String url = apiBase() + "/v2/payments";
			long cents = amount.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValue();
			StringBuilder body = new StringBuilder();
			body.append("{\"amount_money\":{\"amount\":").append(cents).append(",\"currency\":\"USD\"},");
			body.append("\"source_id\":\"").append(sourceId).append("\",");
			body.append("\"autocomplete\":true,");
			body.append("\"customer_id\":\"").append(customerId).append("\",");
			body.append("\"reference_id\":\"").append(studentId).append("\"");
			if (verificationToken != null && !verificationToken.isBlank()) {
				body.append(",\"verification_token\":\"").append(verificationToken).append("\"");
			}
			if (accountHolderName != null && !accountHolderName.isBlank()) {
				body.append(",\"buyer_email_address\":\"").append(email).append("\"");
			}
			body.append("}");

			HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Authorization", "Bearer " + apiToken);
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Square-Version", "2025-10-16");
			conn.setDoOutput(true);
			try (OutputStream os = conn.getOutputStream()) {
				os.write(body.toString().getBytes(StandardCharsets.UTF_8));
			}
			int code = conn.getResponseCode();
			String resp;
			if (code != 200 && code != 201) {
				InputStream es = conn.getErrorStream();
				String err = es != null ? new String(es.readAllBytes(), StandardCharsets.UTF_8) : "";
				throw new SquarePaymentException(code, "PAYMENT_FAILED", "API", "Payment failed: " + err);
			}
			try (InputStream is = conn.getInputStream()) {
				resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			}
			String paymentId = jsonGet(resp, "payment", "id");
			String status = jsonGet(resp, "payment", "status");
			String receiptUrl = jsonGet(resp, "payment", "receipt_url");
			if (paymentId == null || paymentId.isEmpty()) {
				paymentId = extract(resp, "\"id\":\"");
			}
			return new PaymentResult(paymentId, status, receiptUrl);
		} catch (Exception ex) {
			throw new SquarePaymentException(500, "SQUARE_API_ERROR", "API", "Failed to take payment: " + ex.getMessage());
		}
	}

	private String createCustomer(String email) throws Exception {
		String createUrl = apiBase() + "/v2/customers";
		String createBody = "{\"email_address\":\"" + email + "\"}";
		HttpURLConnection conn = (HttpURLConnection) URI.create(createUrl).toURL().openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Authorization", "Bearer " + apiToken);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Square-Version", "2025-10-16");
		conn.setDoOutput(true);
		try (OutputStream os = conn.getOutputStream()) {
			os.write(createBody.getBytes(StandardCharsets.UTF_8));
		}
		int code = conn.getResponseCode();
		if (code != 200 && code != 201) {
			InputStream es = conn.getErrorStream();
			String err = es != null ? new String(es.readAllBytes(), StandardCharsets.UTF_8) : "";
			throw new RuntimeException("Customer create failed: " + code + " - " + err);
		}
		String resp;
		try (InputStream is = conn.getInputStream()) {
			resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
		String customerId = jsonGet(resp, "customer", "id");
		if (customerId == null || customerId.isEmpty()) {
			customerId = extract(resp, "\"id\":\"");
		}
		if (customerId == null || customerId.isEmpty()) {
			throw new RuntimeException("Failed to extract customer ID from response: " + resp);
		}
		return customerId;
	}

	private String attachCard(String customerId, String cardToken) throws Exception {
		String cardUrl = apiBase() + "/v2/customers/" + customerId + "/cards";
		String cardBody = "{\"card_nonce\":\"" + cardToken + "\"}";
		HttpURLConnection conn = (HttpURLConnection) URI.create(cardUrl).toURL().openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Authorization", "Bearer " + apiToken);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Square-Version", "2025-10-16");
		conn.setDoOutput(true);
		try (OutputStream os = conn.getOutputStream()) {
			os.write(cardBody.getBytes(StandardCharsets.UTF_8));
		}
		int code = conn.getResponseCode();
		if (code != 200 && code != 201) {
			InputStream es = conn.getErrorStream();
			String err = es != null ? new String(es.readAllBytes(), StandardCharsets.UTF_8) : "";
			throw new RuntimeException("Card attach failed: " + code + " - " + err);
		}
		String resp;
		try (InputStream is = conn.getInputStream()) {
			resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
		String cardId = jsonGet(resp, "card", "id");
		if (cardId == null || cardId.isEmpty()) {
			cardId = extract(resp, "\"id\":\"");
		}
		if (cardId == null || cardId.isEmpty()) {
			throw new RuntimeException("Failed to extract card ID from response: " + resp);
		}
		return cardId;
	}

	private String extract(String json, String key) {
		int idx = json.indexOf(key);
		if (idx < 0) return null;
		int start = idx + key.length();
		int end = json.indexOf('"', start);
		if (end < 0) return null;
		return json.substring(start, end);
	}

	private String jsonGet(String json, String... path) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(json);
			for (String p : path) {
				if (node == null) return null;
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
