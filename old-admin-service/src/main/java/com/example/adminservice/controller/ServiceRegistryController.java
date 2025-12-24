package com.example.adminservice.controller;

import com.example.adminservice.model.ManagedService;
import com.example.adminservice.service.ServiceRegistryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = "*")
public class ServiceRegistryController {
    @Autowired
    private ServiceRegistryService serviceRegistryService;

    @PostMapping("/register")
    public ResponseEntity<ManagedService> registerService(@RequestBody Map<String, String> request) {
        ManagedService service = serviceRegistryService.registerService(
                request.get("serviceName"),
                request.get("serviceUrl"),
                request.get("servicePort"),
                request.get("description")
        );
        return ResponseEntity.ok(service);
    }

    @GetMapping
    public ResponseEntity<List<ManagedService>> getAllServices() {
        return ResponseEntity.ok(serviceRegistryService.getAllServices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ManagedService> getServiceById(@PathVariable Long id) {
        return ResponseEntity.ok(new ManagedService());
    }

    @GetMapping("/{id}/health")
    public ResponseEntity<Map<String, Object>> checkServiceHealth(@PathVariable Long id) {
        boolean healthy = serviceRegistryService.checkServiceHealth(id);
        return ResponseEntity.ok(Map.of("healthy", healthy, "timestamp", System.currentTimeMillis()));
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<Map<String, Object>> getServiceLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "100") int lines) {
        return ResponseEntity.ok(serviceRegistryService.getServiceLogs(id, lines));
    }

    @GetMapping("/{id}/metrics")
    public ResponseEntity<Map<String, Object>> getServiceMetrics(@PathVariable Long id) {
        return ResponseEntity.ok(serviceRegistryService.getServiceMetrics(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ManagedService> updateService(@PathVariable Long id, @RequestBody Map<String, String> request) {
        ManagedService service = serviceRegistryService.updateService(
                id,
                request.get("serviceName"),
                request.get("serviceUrl"),
                request.get("servicePort"),
                request.get("description")
        );
        return ResponseEntity.ok(service);
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<String> enableService(@PathVariable Long id) {
        serviceRegistryService.enableService(id);
        return ResponseEntity.ok("Service enabled");
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<String> disableService(@PathVariable Long id) {
        serviceRegistryService.disableService(id);
        return ResponseEntity.ok("Service disabled");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteService(@PathVariable Long id) {
        serviceRegistryService.deleteService(id);
        return ResponseEntity.ok("Service deleted");
    }
}
