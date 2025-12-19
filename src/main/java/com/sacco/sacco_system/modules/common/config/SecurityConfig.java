package com.sacco.sacco_system.modules.common.config;

import com.sacco.sacco_system.modules.common.security.JwtAuthenticationFilter;
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
                        // ====================================================
                        // 1. PUBLIC ENDPOINTS
                        // ====================================================
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/auth/**", "/api/verify/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/settings").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // ====================================================
                        // 2. VOTING (FIXED: Allow Members + Officials)
                        // ====================================================
                        // This was causing the 403. We allow MEMBER and officials here.
                        .requestMatchers("/api/loans/*/vote").hasAnyAuthority("MEMBER", "CHAIRPERSON", "TREASURER", "SECRETARY", "ADMIN")

                        // New: Allow members to see active agenda items to vote on
                        .requestMatchers("/api/loans/agenda/active").hasAnyAuthority("MEMBER", "CHAIRPERSON", "TREASURER", "SECRETARY", "ADMIN")

                        // ====================================================
                        // 3. LOAN OFFICER ACTIONS
                        // ====================================================
                        .requestMatchers("/api/loans/*/review").hasAnyAuthority("LOAN_OFFICER", "ADMIN")
                        .requestMatchers("/api/loans/*/approve").hasAnyAuthority("LOAN_OFFICER", "ADMIN")
                        .requestMatchers("/api/loans/*/reject").hasAnyAuthority("LOAN_OFFICER", "ADMIN")

                        // ====================================================
                        // 4. GOVERNANCE (Secretary / Chair)
                        // ====================================================
                        .requestMatchers("/api/loans/*/table").hasAnyAuthority("SECRETARY", "ADMIN")
                        .requestMatchers("/api/loans/*/finalize").hasAnyAuthority("SECRETARY", "ADMIN")
                        .requestMatchers("/api/loans/*/start-voting").hasAnyAuthority("CHAIRPERSON", "ADMIN")

                        // ====================================================
                        // 5. FINANCE (Treasurer)
                        // ====================================================
                        .requestMatchers("/api/loans/*/disburse").hasAnyAuthority("TREASURER", "ADMIN")
                        .requestMatchers("/api/finance/**").hasAnyAuthority("TREASURER", "ADMIN")

                        // ====================================================
                        // 6. ADMIN
                        // ====================================================
                        .requestMatchers("/api/admin/**", "/api/settings/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/loans/*/admin-approve").hasAuthority("ADMIN")

                        // ====================================================
                        // 7. GENERAL ACCESS
                        // ====================================================
                        .requestMatchers("/api/loans/my-loans").authenticated()

                        // Only Staff/Admin can view ALL loans
                        .requestMatchers(HttpMethod.GET, "/api/loans").hasAnyAuthority("LOAN_OFFICER", "ADMIN", "TREASURER", "CHAIRPERSON", "SECRETARY")

                        // ====================================================
                        // 8. CATCH-ALL
                        // ====================================================
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
        configuration.setAllowedOrigins(List.of("*")); // In production, replace * with your frontend domain
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}