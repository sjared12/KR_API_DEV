package com.example.simpletixwebhook.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Controller for serving documentation files to authenticated users
 */
@Controller
@RequestMapping("/docs")
public class DocumentationController {

    private static final String DOCS_BASE_PATH = "subscription-docs/";
    
    /**
     * Serve the documentation viewer HTML page
     */
    @GetMapping({"", "/", "/index"})
    public String docsIndex() {
        return "forward:/docs.html";
    }

    /**
     * Get list of available documentation files
     */
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<?> listDocs() {
        try {
            // Get docs from classpath (inside JAR when deployed)
            List<Map<String, String>> docs = new ArrayList<>();
            
            // Define documentation files with titles
            Map<String, String> docFiles = new LinkedHashMap<>();
            docFiles.put("README.md", "Getting Started");
            docFiles.put("SUBSCRIPTION_QUICK_START.md", "Quick Start Guide");
            docFiles.put("SUBSCRIPTION_MANAGEMENT.md", "Subscription Management");
            docFiles.put("API_EXAMPLES.md", "API Examples");
            docFiles.put("SUBSCRIPTION_GUI_GUIDE.md", "GUI User Guide");
            docFiles.put("PROJECT_ARCHITECTURE.md", "Project Architecture");
            docFiles.put("IMPLEMENTATION_SUMMARY.md", "Implementation Summary");
            docFiles.put("IMPLEMENTATION_COMPLETE.md", "Implementation Details");
            docFiles.put("DEPLOYMENT_GUIDE.md", "Deployment Guide");
            docFiles.put("SUBSCRIPTION_REORGANIZATION.md", "Code Organization");
            
            for (Map.Entry<String, String> entry : docFiles.entrySet()) {
                String filename = entry.getKey();
                String title = entry.getValue();
                
                try {
                    Resource resource = new ClassPathResource(DOCS_BASE_PATH + filename);
                    if (resource.exists()) {
                        Map<String, String> doc = new HashMap<>();
                        doc.put("filename", filename);
                        doc.put("title", title);
                        doc.put("path", "/docs/api/file/" + filename);
                        docs.add(doc);
                    }
                } catch (Exception e) {
                    // Skip files that don't exist
                }
            }
            
            return ResponseEntity.ok(docs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to list documentation files: " + e.getMessage()));
        }
    }

    /**
     * Get content of a specific documentation file
     */
    @GetMapping("/api/file/{filename}")
    @ResponseBody
    public ResponseEntity<?> getDocFile(@PathVariable String filename) {
        try {
            // Security: only allow .md files and prevent path traversal
            if (!filename.endsWith(".md") || filename.contains("..") || filename.contains("/")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid filename"));
            }

            Resource resource = new ClassPathResource(DOCS_BASE_PATH + filename);
            
            if (!resource.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Documentation file not found"));
            }

            try (InputStream is = resource.getInputStream()) {
                String content = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
                
                Map<String, Object> response = new HashMap<>();
                response.put("filename", filename);
                response.put("content", content);
                
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
            }
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to read file: " + e.getMessage()));
        }
    }
}
