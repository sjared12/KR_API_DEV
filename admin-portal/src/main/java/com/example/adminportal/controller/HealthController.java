package com.example.adminportal.controller;

import com.example.adminportal.service.DigitalOceanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Health and status endpoints
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class HealthController {

    private final DigitalOceanService digitalOceanService;

    /**
     * GET /api/health - Service health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(digitalOceanService.getHealth());
    }

    /**
     * GET /api/do/validate - Validate DigitalOcean configuration
     */
    @GetMapping("/do/validate")
    public ResponseEntity<Map<String, Object>> validateDigitalOceanConfig() {
        try {
            return ResponseEntity.ok(digitalOceanService.validateConfig());
        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("ok", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("ok", false);
            error.put("error", "Set DO_API_TOKEN and DO_APP_ID");
            return ResponseEntity.status(400).body(error);
        }
    }

    /**
     * GET /api/do/apps - List DigitalOcean App Platform applications
     */
    @GetMapping("/do/apps")
    public ResponseEntity<List<Map<String, Object>>> listApps() {
        try {
            return ResponseEntity.ok(digitalOceanService.listApps());
        } catch (IOException e) {
            log.error("Error listing apps", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * GET /api/admin/users - Static admin users endpoint (placeholder)
     */
    @GetMapping("/admin/users")
    public ResponseEntity<List<Map<String, Object>>> adminUsers() {
        List<Map<String, Object>> users = List.of(
            Map.of("id", 1, "name", "Alice Admin", "email", "alice@example.com", "role", "admin"),
            Map.of("id", 2, "name", "Bob Manager", "email", "bob@example.com", "role", "manager"),
            Map.of("id", 3, "name", "Carol Accountant", "email", "carol@example.com", "role", "accountant")
        );
        return ResponseEntity.ok(users);
    }
}
