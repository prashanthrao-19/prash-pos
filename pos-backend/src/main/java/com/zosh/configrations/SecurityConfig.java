package com.zosh.configrations;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Autowired
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:5000}")
    private String allowedOriginsConfig;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(Authorize -> Authorize
                        .requestMatchers(HttpMethod.GET, "/api/super-admin/subscription-plans/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/subscription-plans/**").permitAll()
                        .requestMatchers("/api/super-admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/subscription-plans/**").hasRole("ADMIN")
                        .requestMatchers("/api/chat/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(new JwtValidator(jwtSecret), BasicAuthenticationFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(
                        exceptionHandler -> exceptionHandler
                                .authenticationEntryPoint(customAuthenticationEntryPoint))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        return new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                CorsConfiguration cfg = new CorsConfiguration();

                List<String> origins = new ArrayList<>();
                if (allowedOriginsConfig != null && !allowedOriginsConfig.isBlank()) {
                    for (String origin : allowedOriginsConfig.split(",")) {
                        String trimmed = origin.trim();
                        if (!trimmed.isEmpty()) {
                            origins.add(trimmed);
                        }
                    }
                }

                // Default fallbacks if empty
                if (origins.isEmpty()) {
                    origins.add("http://localhost:3000");
                    origins.add("http://localhost:5173");
                    origins.add("http://localhost:5000");
                }

                cfg.setAllowedOrigins(origins);
                cfg.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                cfg.setAllowCredentials(true);
                cfg.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
                cfg.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
                cfg.setMaxAge(3600L);
                return cfg;
            }
        };
    }
}
