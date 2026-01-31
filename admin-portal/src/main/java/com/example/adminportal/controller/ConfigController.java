package com.example.adminportal.controller;

import com.example.adminportal.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration management endpoints
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ConfigService configService;

    /**
     * GET /api/config - Get all configuration
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllConfig() {
        return ResponseEntity.ok(configService.getConfig());
    }

    /**
     * GET /api/config/:service - Get configuration for a specific service
     */
    @GetMapping("/{service}")
    public ResponseEntity<Object> getServiceConfig(@PathVariable String service) {
        Object config = configService.getServiceConfig(service);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }

    /**
     * PUT /api/config/:service - Update service configuration
     */
    @PutMapping("/{service}")
    public ResponseEntity<Map<String, Object>> updateServiceConfig(
        @PathVariable String service,
        @RequestBody Map<String, Object> updates) {
        try {
            configService.updateServiceConfig(service, updates);
            Object updatedConfig = configService.getServiceConfig(service);
            
            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("config", updatedConfig);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/public-config/:service - Public endpoint to fetch service configuration
     * Used by client-side apps to discover API endpoints
     */
    @GetMapping("/public/{service}")
    public ResponseEntity<Object> getPublicServiceConfig(@PathVariable String service) {
        Object config = configService.getServiceConfig(service);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }
}
