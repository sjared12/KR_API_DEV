package com.example.adminservice.controller;

import com.example.adminservice.model.AdminUser;
import com.example.adminservice.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*")
public class AdminUserController {
    @Autowired
    private AdminUserService adminUserService;

    @PostMapping("/create")
    public ResponseEntity<AdminUser> createUser(@RequestBody Map<String, String> request) {
        AdminUser user = adminUserService.createUser(
                request.get("username"),
                request.get("password"),
                request.getOrDefault("role", "ROLE_OPERATOR")
        );
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<List<AdminUser>> getAllUsers() {
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUser> getUserById(@PathVariable Long id) {
        // Implementation for getting user by ID
        return ResponseEntity.ok(new AdminUser());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminUser> updateUser(@PathVariable Long id, @RequestBody Map<String, String> request) {
        AdminUser user = adminUserService.updateUser(
                id,
                request.get("username"),
                request.get("role"),
                Boolean.parseBoolean(request.get("active"))
        );
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully");
    }
}
