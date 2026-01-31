package com.example.simpletixwebhook.config;

import com.example.simpletixwebhook.model.Permission;
import com.example.simpletixwebhook.model.User;
import com.example.simpletixwebhook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Initializes default admin user in database on application startup
 */
@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.user:admin}")
    private String adminUser;

    @Value("${app.admin.pass:changeit}")
    private String adminPass;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Check if admin user already exists
        if (userRepository.existsByUsername(adminUser)) {
            log.info("Admin user '{}' already exists, skipping initialization", adminUser);
            return;
        }

        try {
            // Create default admin user with all permissions
            User adminUserEntity = new User(
                adminUser,
                passwordEncoder.encode(adminPass),
                "admin@example.com",
                "System Administrator"
            );

            // Grant all permissions to admin
            Set<Permission> adminPermissions = new HashSet<>();
            adminPermissions.add(Permission.VIEW_SUBSCRIPTIONS);
            adminPermissions.add(Permission.CANCEL_SUBSCRIPTIONS);
            adminPermissions.add(Permission.REQUEST_REFUNDS);
            adminPermissions.add(Permission.APPROVE_REFUNDS);
            adminPermissions.add(Permission.VIEW_REFUNDS);
            adminPermissions.add(Permission.MANAGE_USERS);
            adminPermissions.add(Permission.VIEW_USERS);
            adminPermissions.add(Permission.SYSTEM_ADMIN);

            adminUserEntity.setPermissions(adminPermissions);
            adminUserEntity.setActive(true);
            adminUserEntity.setLastLogin(LocalDateTime.now());

            userRepository.save(adminUserEntity);
            log.info("Default admin user '{}' created successfully", adminUser);

        } catch (Exception e) {
            log.error("Error initializing default admin user", e);
        }
    }
}
