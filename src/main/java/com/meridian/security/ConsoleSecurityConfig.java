package com.meridian.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ConsoleSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain consoleSecurity(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/console/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST, "/api/console/changes/*/approve",
                                "/api/console/changes/*/reject", "/api/console/rules/rollback")
                        .hasRole(Roles.APPROVER)
                        .requestMatchers(HttpMethod.POST, "/api/console/changes", "/api/console/changes/*/dry-run",
                                "/api/console/changes/*/submit", "/api/console/contracts/imports",
                                "/api/console/parcels", "/api/console/events", "/api/console/simulations")
                        .hasRole(Roles.OPERATOR)
                        .requestMatchers(HttpMethod.POST, "/api/console/routes/resolve")
                        .hasAnyRole(Roles.VIEWER, Roles.OPERATOR, Roles.APPROVER)
                        .requestMatchers(HttpMethod.GET, "/api/console/**")
                        .hasAnyRole(Roles.VIEWER, Roles.OPERATOR, Roles.APPROVER)
                        .anyRequest().denyAll())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService consoleUsers(PasswordEncoder encoder, @Value("${meridian.demo.password}") String password) {
        String encoded = encoder.encode(password);
        return new InMemoryUserDetailsManager(
                User.withUsername("olivia").password(encoded).roles(Roles.OPERATOR).build(),
                User.withUsername("omar").password(encoded).roles(Roles.OPERATOR).build(),
                User.withUsername("anna").password(encoded).roles(Roles.APPROVER).build(),
                User.withUsername("victor").password(encoded).roles(Roles.VIEWER).build(),
                User.withUsername("nordlicht").password(encoded).roles(Roles.BUSINESS).build(),
                User.withUsername("weserkontor").password(encoded).roles(Roles.BUSINESS).build());
    }
}
