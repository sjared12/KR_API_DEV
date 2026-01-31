package com.example.adminportal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Placeholder container/service management endpoints
 * These would integrate with DigitalOcean App Platform API for real container management
 */
@RestController
@RequestMapping("/api/containers")
public class ContainerController {

    /**
     * GET /api/containers - List all containers/services
     * Currently returns empty list; extend with DigitalOcean integration if needed
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listContainers() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Container management not yet implemented");
        response.put("containers", java.util.Collections.emptyList());
        return ResponseEntity.ok(response);
    }
}
