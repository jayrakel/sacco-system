package com.sacco.sacco_system.config;

import com.sacco.sacco_system.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // 1. PUBLIC ENDPOINTS
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/auth/**", "/api/verify/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/settings").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // 2. ✅ LOAN OFFICER RESTRICTIONS (The Fix)
                        // Only Loan Officers can Review, Approve, or Reject at the first stage
                        .requestMatchers("/api/loans/*/review").hasAuthority("LOAN_OFFICER")
                        .requestMatchers("/api/loans/*/approve").hasAnyAuthority("LOAN_OFFICER", "ADMIN") // Admins might need override
                        .requestMatchers("/api/loans/*/reject").hasAnyAuthority("LOAN_OFFICER", "ADMIN")

                        // 3. ✅ FINANCE / TREASURER RESTRICTIONS
                        .requestMatchers("/api/loans/*/disburse").hasAnyAuthority("TREASURER", "ADMIN")
                        .requestMatchers("/api/finance/**").hasAnyAuthority("TREASURER", "ADMIN")

                        // 4. ✅ ADMIN RESTRICTIONS
                        .requestMatchers("/api/admin/**", "/api/settings/**").hasAuthority("ADMIN")

                        // 5. MEMBER & SHARED
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        //TODO: FIX THIS BEFORE PRODUCTION
        configuration.setAllowedOrigins(List.of("*")); //❌DANGEROUS
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}