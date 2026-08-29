package com.tourswitch.global.security;

import com.tourswitch.global.security.handler.CustomAccessDeniedHandler;
import com.tourswitch.global.security.handler.CustomAuthenticationEntryPoint;
import com.tourswitch.global.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http
    ) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)

            .formLogin(AbstractHttpConfigurer::disable)

            .httpBasic(AbstractHttpConfigurer::disable)

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .exceptionHandling(exception ->
                exception
                    .authenticationEntryPoint(
                        authenticationEntryPoint
                    )
                    .accessDeniedHandler(
                        accessDeniedHandler
                    )
            )

            .authorizeHttpRequests(auth ->
                auth
                    .requestMatchers(
                        HttpMethod.OPTIONS,
                        "/**"
                    ).permitAll()

                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/auth/login",
                        "/api/auth/refresh"
                    ).permitAll()

                    .requestMatchers(
                        "/api/users/me",
                        "/api/auth/logout"
                    ).authenticated()

                    .anyRequest().permitAll()
            )

            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}