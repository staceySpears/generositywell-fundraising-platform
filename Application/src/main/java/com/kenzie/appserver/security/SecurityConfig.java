package com.kenzie.appserver.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless Spring Security configuration.
 * CSRF is disabled (no session cookies), and all authentication is via JWT in the
 * {@code Authorization: Bearer} header.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Defines the security filter chain: public endpoints, JWT filter, and a 401
     * entry point for unauthenticated requests to protected routes.
     *
     * @param http the HttpSecurity builder
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // public read access to campaigns, events, and user profiles
                        .requestMatchers(HttpMethod.GET, "/campaigns/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/events/**").permitAll()
                        // giving history and RSVP history are self-only — must come before the broad users permit
                        .requestMatchers(HttpMethod.GET, "/users/*/donations").authenticated()
                        .requestMatchers(HttpMethod.GET, "/users/*/rsvps").authenticated()
                        .requestMatchers(HttpMethod.GET, "/users/**").permitAll()
                        // donations are public so supporters don't need an account (Stripe handles identity)
                        .requestMatchers(HttpMethod.POST, "/campaigns/*/donate").permitAll()
                        // registration and login are always open
                        .requestMatchers(HttpMethod.POST, "/users").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        // Stripe webhook — authenticated by signature, not JWT
                        .requestMatchers("/webhooks/stripe").permitAll()
                        // Swagger UI for local dev
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // everything else requires a valid JWT
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Provides a BCrypt password encoder with default strength (10 rounds).
     *
     * @return the password encoder bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
