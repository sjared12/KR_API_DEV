package com.example.simpletixwebhook.service;

import org.springframework.http.HttpStatus;

public class SquarePaymentException extends RuntimeException {
    private final int statusCode;
    private final String errorCode;
    private final String category;

    public SquarePaymentException(int statusCode, String errorCode, String category, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.category = category;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getCategory() {
        return category;
    }

    public HttpStatus toHttpStatus() {
        if (statusCode >= 400 && statusCode < 600) {
            HttpStatus resolved = HttpStatus.resolve(statusCode);
            if (resolved == HttpStatus.NOT_FOUND && "INVALID_REQUEST_ERROR".equals(category)) {
                return HttpStatus.BAD_REQUEST;
            }
            return resolved != null ? resolved : mapCategory();
        }
        return mapCategory();
    }

    private HttpStatus mapCategory() {
        if (category == null) {
            return HttpStatus.BAD_GATEWAY;
        }
        return switch (category) {
            case "INVALID_REQUEST_ERROR" -> HttpStatus.BAD_REQUEST;
            case "CARD_ERROR", "PAYMENT_METHOD_ERROR" -> HttpStatus.PAYMENT_REQUIRED;
            case "RATE_LIMITED_ERROR" -> HttpStatus.TOO_MANY_REQUESTS;
            case "AUTHENTICATION_ERROR" -> HttpStatus.UNAUTHORIZED;
            case "INVALID_STATE_ERROR" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_GATEWAY;
        };
    }
}
