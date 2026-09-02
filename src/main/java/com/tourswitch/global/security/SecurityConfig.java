package com.tourswitch.global.security;

import com.tourswitch.global.config.security.FrontendProperties;
import com.tourswitch.global.security.handler.CustomAccessDeniedHandler;
import com.tourswitch.global.security.handler.CustomAuthenticationEntryPoint;
import com.tourswitch.global.security.jwt.JwtAuthenticationFilter;
import com.tourswitch.global.security.oauth2.CustomOAuth2UserService;
import com.tourswitch.global.security.oauth2.OAuth2FailureHandler;
import com.tourswitch.global.security.oauth2.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oauth2SuccessHandler;
    private final OAuth2FailureHandler oauth2FailureHandler;

    private final FrontendProperties frontendProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http
    ) {
        http
            .cors(cors ->
                cors.configurationSource(
                    corsConfigurationSource()
                )
            )

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
                        "/api/auth/oauth2/authorization/**",
                        "/api/auth/oauth2/callback/**"
                    ).permitAll()

                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/auth/refresh"
                    ).permitAll()

                    .requestMatchers(
                        "/api/users/me",
                        "/api/auth/logout"
                    ).authenticated()

                    .anyRequest().permitAll()
            )

            .oauth2Login(oauth ->
                oauth
                    .authorizationEndpoint(endpoint ->
                        endpoint.baseUri(
                            "/api/auth/oauth2/authorization"
                        )
                    )

                    .redirectionEndpoint(endpoint ->
                        endpoint.baseUri(
                            "/api/auth/oauth2/callback/*"
                        )
                    )

                    .userInfoEndpoint(userInfo ->
                        userInfo.userService(
                            customOAuth2UserService
                        )
                    )

                    .successHandler(
                        oauth2SuccessHandler
                    )

                    .failureHandler(
                        oauth2FailureHandler
                    )
            )

            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration =
            new CorsConfiguration();

        configuration.setAllowedOrigins(
            List.of(
                frontendProperties.origin()
            )
        );

        configuration.setAllowedMethods(
            List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
            )
        );

        configuration.setAllowedHeaders(
            List.of(
                "Authorization",
                "Content-Type",
                "Accept"
            )
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
            "/**",
            configuration
        );

        return source;
    }
}