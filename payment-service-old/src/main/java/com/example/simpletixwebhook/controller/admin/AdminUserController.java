package com.example.simpletixwebhook.controller.admin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.simpletixwebhook.model.Permission;
import com.example.simpletixwebhook.model.User;
import com.example.simpletixwebhook.service.UserService;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*")
public class AdminUserController {
    private final UserService userService;
    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<Map<String, Object>> listUsers() {
        return userService.getAllUsers().stream().map(user -> Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "fullName", user.getFullName(),
            "active", user.isActive(),
            "permissions", user.getPermissions().stream().map(Permission::name).collect(Collectors.toList())
        )).collect(Collectors.toList());
    }

    @PostMapping("/{id}/permissions")
    public ResponseEntity<?> updatePermissions(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object permsObj = body.get("permissions");
        if (!(permsObj instanceof List<?> permsList)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid permissions list"));
        }
        Set<Permission> perms = permsList.stream()
            .map(Object::toString)
            .map(String::toUpperCase)
            .map(Permission::valueOf)
            .collect(Collectors.toSet());
        Optional<User> userOpt = userService.getUserById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }
        User user = userOpt.get();
        user.setPermissions(perms);
        userService.updateUser(user);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
