package com.example.simpletixwebhook.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.admin.user:admin}")
    private String adminUser;

    @Value("${app.admin.pass:changeit}")
    private String adminPass;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/pay",
                    "/pay/**",
                    "/pay.html",
                    "/pay-error",
                    "/pay-error.html",
                    "/api/payments/**",
                    "/api/auth/**",
                    "/api/subscriptions/**",
                    "/api/refunds/**",
                    "/charge",
                    "/api/charge",
                    "/api/plans",
                    "/api/plans/**",
                    "/subscription-manager",
                    "/subscription-manager.html",
                    "/subscription-manager/**",
                    "/docs",
                    "/docs/**",
                    "/docs.html",
                    "/documentation",
                    "/admin/**",
                    "/static/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/webjars/**"
                ).permitAll()
                .anyRequest().permitAll()
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withUsername(adminUser)
            .password("{noop}" + adminPass)
            .roles("ADMIN")
            .build();
        return new InMemoryUserDetailsManager(user);
    }
}
