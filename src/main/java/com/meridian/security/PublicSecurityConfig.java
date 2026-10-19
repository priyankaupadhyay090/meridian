package com.meridian.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class PublicSecurityConfig {

    @Bean
    @Order(2)
    SecurityFilterChain publicSecurity(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/public/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.GET, "/api/public/couriers", "/api/public/contract-checks/examples",
                                "/api/public/rules", "/api/public/rule-changes/examples")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/public/quotes", "/api/public/checkout-options",
                                "/api/public/contract-checks", "/api/public/contract-checks/documents",
                                "/api/public/rule-changes/test-run")
                        .permitAll()
                        .anyRequest().denyAll())
                .build();
    }

    @Bean
    @Order(0)
    SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/actuator/**")
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/prometheus").permitAll()
                        .anyRequest().denyAll())
                .build();
    }

    @Bean
    @Order(10)
    SecurityFilterChain denyEverythingElse(HttpSecurity http) throws Exception {
        return http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/error").permitAll()
                        .anyRequest().denyAll())
                .build();
    }
}
