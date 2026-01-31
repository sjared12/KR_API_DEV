package com.example.adminportal.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for interacting with DigitalOcean App Platform API
 */
@Slf4j
@Service
public class DigitalOceanService {

    private final String doApiToken;
    private final String doAppId;
    private final String doApiBase = "https://api.digitalocean.com/v2";
    private final RestTemplate restTemplate;

    public DigitalOceanService(
        @Value("${digitalocean.api-token:}") String doApiToken,
        @Value("${digitalocean.app-id:}") String doAppId) {
        this.doApiToken = doApiToken;
        this.doAppId = doAppId;
        this.restTemplate = new RestTemplate();

        if (!doApiToken.isEmpty()) {
            log.info("DigitalOcean API token configured");
        } else {
            log.warn("DigitalOcean API token not configured");
        }

        if (!doAppId.isEmpty()) {
            log.info("DigitalOcean App ID configured: {}…{}", doAppId.substring(0, 6), doAppId.substring(doAppId.length() - 4));
        } else {
            log.warn("DigitalOcean App ID not configured");
        }
    }

    public boolean isConfigured() {
        return !doApiToken.isEmpty() && !doAppId.isEmpty();
    }

    public Map<String, Object> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "ok");
        health.put("doConfigured", !doApiToken.isEmpty());
        health.put("appIdConfigured", !doAppId.isEmpty());

        if (!doAppId.isEmpty()) {
            String masked = doAppId.substring(0, 6) + "…" + doAppId.substring(doAppId.length() - 4);
            health.put("appIdPreview", masked);
        }

        return health;
    }

    public Map<String, Object> validateConfig() throws IOException {
        if (doApiToken.isEmpty() || doAppId.isEmpty()) {
            throw new IllegalArgumentException("DO_API_TOKEN and DO_APP_ID must be set");
        }

        Map<String, Object> app = callDigitalOceanAPI("/apps/" + doAppId);
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        
        if (app != null && app.containsKey("app")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> appData = (Map<String, Object>) app.get("app");
            result.put("name", appData.getOrDefault("id", "unknown"));
            result.put("id", appData.get("id"));
        }

        return result;
    }

    public List<Map<String, Object>> listApps() throws IOException {
        if (doApiToken.isEmpty()) {
            log.warn("DO API not configured, returning empty list");
            return new ArrayList<>();
        }

        try {
            Map<String, Object> response = callDigitalOceanAPI("/apps");
            if (response != null && response.containsKey("apps")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> apps = (List<Map<String, Object>>) response.get("apps");
                return apps != null ? apps : new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("Error listing apps", e);
            if (doAppId != null && !doAppId.isEmpty()) {
                log.warn("Falling back to DO_APP_ID");
                try {
                    Map<String, Object> response = callDigitalOceanAPI("/apps/" + doAppId);
                    if (response != null && response.containsKey("app")) {
                        List<Map<String, Object>> apps = new ArrayList<>();
                        @SuppressWarnings("unchecked")
                        Map<String, Object> app = (Map<String, Object>) response.get("app");
                        apps.add(app);
                        return apps;
                    }
                } catch (Exception ex) {
                    log.error("Fallback also failed", ex);
                }
            }
        }

        return new ArrayList<>();
    }

    public Map<String, Object> getApp(String appId) throws IOException {
        return callDigitalOceanAPI("/apps/" + appId);
    }

    private Map<String, Object> callDigitalOceanAPI(String endpoint) throws IOException {
        String url = doApiBase + endpoint;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + doApiToken);
        headers.set("Content-Type", "application/json");
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling DigitalOcean API: {}", endpoint, e);
            throw new IOException("DigitalOcean API error: " + e.getMessage(), e);
        }
    }
}
