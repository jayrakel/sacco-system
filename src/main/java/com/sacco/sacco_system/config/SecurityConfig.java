package com.sacco.sacco_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.web.cors.CorsConfiguration; // Import this
import org.springframework.web.cors.CorsConfigurationSource; // Import this
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // Import this
import java.util.List; // Import this

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // <--- Enable CORS here
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").permitAll()
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    // Define the CORS rules
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 1. Allow specific frontends (e.g., React on localhost:3000 or localhost:5173)
        // Use "*" to allow ALL for development, but specify exact URLs for production
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:4200", "http://localhost:5173"));

        // 2. Allow specific HTTP methods
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 3. Allow headers (needed for sending JSON data and Auth tokens)
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // 4. Apply this to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}