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
                        // ====================================================
                        // 1. PUBLIC ENDPOINTS (Specific Rules FIRST)
                        // ====================================================
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/auth/**", "/api/verify/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/settings").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // ====================================================
                        // 2. LOAN OFFICER RESTRICTIONS
                        // ====================================================
                        // Actions only for Officers
                        .requestMatchers("/api/loans/*/review").hasAuthority("LOAN_OFFICER")
                        .requestMatchers("/api/loans/*/approve").hasAnyAuthority("LOAN_OFFICER", "ADMIN")
                        .requestMatchers("/api/loans/*/reject").hasAnyAuthority("LOAN_OFFICER", "ADMIN")

                        // ====================================================
                        // 3. FINANCE / TREASURER RESTRICTIONS
                        // ====================================================
                        .requestMatchers("/api/loans/*/disburse").hasAnyAuthority("TREASURER", "ADMIN")
                        .requestMatchers("/api/finance/**").hasAnyAuthority("TREASURER", "ADMIN")

                        // ====================================================
                        // 4. SECRETARY RESTRICTIONS (Fixes your 403 Error)
                        // ====================================================
                        .requestMatchers("/api/loans/*/table").hasAnyAuthority("SECRETARY", "ADMIN")

                        // ====================================================
                        // 5. CHAIRPERSON RESTRICTIONS
                        // ====================================================
                        .requestMatchers("/api/loans/*/start-voting").hasAnyAuthority("CHAIRPERSON", "ADMIN")

                        // ====================================================
                        // 6. ADMIN RESTRICTIONS
                        // ====================================================
                        .requestMatchers("/api/admin/**", "/api/settings/**").hasAuthority("ADMIN")

                        // ====================================================
                        // 7. VIEWING DATA (Secure Lists)
                        // ====================================================
                        // Members can ONLY view their own loans
                        .requestMatchers("/api/loans/my-loans").authenticated()

                        // Only Staff can view the full list of loans
                        .requestMatchers(HttpMethod.GET, "/api/loans").hasAnyAuthority("LOAN_OFFICER", "ADMIN", "TREASURER", "CHAIRPERSON", "SECRETARY")

                        // ====================================================
                        // 8. CATCH-ALL (MUST BE LAST)
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
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}