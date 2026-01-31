package com.example.adminportal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing persistent configuration file
 */
@Slf4j
@Service
public class ConfigService {

    private final String configPath;
    private final ObjectMapper objectMapper;
    private Map<String, Object> config;

    public ConfigService(@Value("${app.config.path:/data/config.json}") String configPath) {
        this.configPath = configPath;
        this.objectMapper = new ObjectMapper();
        this.config = new HashMap<>();
        initializeConfig();
    }

    private void initializeConfig() {
        File file = new File(configPath);
        
        // Create directory if it doesn't exist
        File directory = file.getParentFile();
        if (directory != null && !directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                log.info("Created config directory: {}", directory.getAbsolutePath());
            }
        }

        // Load existing config or create defaults
        if (file.exists()) {
            try {
                config = objectMapper.readValue(file, Map.class);
                log.info("Loaded config from {}", configPath);
            } catch (IOException e) {
                log.error("Failed to load config from {}, using defaults", configPath, e);
                config = createDefaultConfig();
                saveConfig();
            }
        } else {
            log.info("Config file not found, creating with defaults at {}", configPath);
            config = createDefaultConfig();
            saveConfig();
        }
    }

    private Map<String, Object> createDefaultConfig() {
        Map<String, Object> defaults = new HashMap<>();
        
        defaults.put("api", Map.of(
            "SPRING_DATASOURCE_URL", "jdbc:postgresql://db:5432/simpletixdb",
            "SPRING_DATASOURCE_USERNAME", "postgres",
            "SPRING_DATASOURCE_PASSWORD", "postgres"
        ));
        
        defaults.put("payment", Map.of(
            "SPRING_DATASOURCE_URL", "jdbc:postgresql://db:5432/payments",
            "SPRING_DATASOURCE_USERNAME", "postgres",
            "SPRING_DATASOURCE_PASSWORD", "postgres"
        ));
        
        defaults.put("logger", new HashMap<>());
        
        defaults.put("feedback", Map.of(
            "API_ENDPOINT", "http://api:8080"
        ));
        
        defaults.put("admin", Map.of(
            "API_BASE", ""
        ));
        
        defaults.put("proxy", new HashMap<>());
        
        return defaults;
    }

    public Map<String, Object> getConfig() {
        return new HashMap<>(config);
    }

    public Object getServiceConfig(String service) {
        return config.get(service);
    }

    public void updateServiceConfig(String service, Map<String, Object> updates) {
        if (!config.containsKey(service)) {
            throw new IllegalArgumentException("Unknown service: " + service);
        }

        Object existing = config.get(service);
        if (existing instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> existingMap = (Map<String, Object>) existing;
            existingMap.putAll(updates);
        } else {
            config.put(service, updates);
        }

        saveConfig();
        log.info("Updated config for service: {}", service);
    }

    private void saveConfig() {
        try {
            File file = new File(configPath);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, config);
            log.info("Saved config to {}", configPath);
        } catch (IOException e) {
            log.error("Failed to save config to {}", configPath, e);
            throw new RuntimeException("Failed to save configuration", e);
        }
    }
}
