package com.example.adminservice.service;

import com.example.adminservice.model.AdminUser;
import com.example.adminservice.repository.AdminUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AdminUserService {
    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AdminUser createUser(String username, String password, String role) {
        if (adminUserRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        AdminUser user = new AdminUser(username, passwordEncoder.encode(password), role);
        return adminUserRepository.save(user);
    }

    public Optional<AdminUser> findByUsername(String username) {
        return adminUserRepository.findByUsername(username);
    }

    public AdminUser updateLastLogin(String username) {
        AdminUser user = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setLastLogin(LocalDateTime.now());
        return adminUserRepository.save(user);
    }

    public List<AdminUser> getAllUsers() {
        return adminUserRepository.findAll();
    }

    public AdminUser updateUser(Long id, String username, String role, Boolean active) {
        AdminUser user = adminUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setUsername(username);
        user.setRole(role);
        user.setActive(active);
        return adminUserRepository.save(user);
    }

    public void deleteUser(Long id) {
        adminUserRepository.deleteById(id);
    }

    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
