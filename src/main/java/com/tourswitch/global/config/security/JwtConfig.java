package com.tourswitch.global.config.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    JwtProperties.class,
    RefreshTokenCookieProperties.class
})
public class JwtConfig {

}
