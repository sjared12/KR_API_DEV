package com.example.adminportal.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/", "/index.html", "/api/health").permitAll()
                    .requestMatchers("/css/**", "/js/**", "/assets/**").permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().permitAll()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractKeycloakAuthorities);
        converter.setPrincipalClaimName("email");
        return converter;
    }

    private Collection<org.springframework.security.core.GrantedAuthority> extractKeycloakAuthorities(Jwt jwt) {
        List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
        
        // Extract from realm_access.roles
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (realmAccess instanceof java.util.Map<?, ?> map) {
            Object rolesObj = map.get("roles");
            if (rolesObj instanceof java.util.Collection<?> roles) {
                roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            }
        }
        
        // Extract from resource_access
        Object resourceAccess = jwt.getClaims().get("resource_access");
        if (resourceAccess instanceof java.util.Map<?, ?> raMap) {
            raMap.values().forEach(client -> {
                if (client instanceof java.util.Map<?, ?> clientMap) {
                    Object rolesObj = clientMap.get("roles");
                    if (rolesObj instanceof java.util.Collection<?> roles) {
                        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
                    }
                }
            });
        }
        
        // Extract from groups
        Object groupsObj = jwt.getClaims().get("groups");
        if (groupsObj instanceof java.util.Collection<?> groups) {
            groups.forEach(group -> {
                String name = String.valueOf(group);
                if (name.contains("/")) {
                    name = name.substring(name.lastIndexOf('/') + 1);
                }
                String normalized = name.trim().toUpperCase().replace(' ', '_');
                switch (normalized) {
                    case "ADMIN" -> authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    case "REFUND_APPROVER" -> authorities.add(new SimpleGrantedAuthority("ROLE_REFUND_APPROVER"));
                    case "END_USER", "USER" -> authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                    default -> authorities.add(new SimpleGrantedAuthority("ROLE_" + normalized));
                }
            });
        }
        
        return (Collection<org.springframework.security.core.GrantedAuthority>) (Collection<?>) authorities;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addAllowedOriginPattern(".*krhscougarband\\.org");
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
