package com.compliance.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
@Getter @Setter
public class JwtProperties {
    private String secret = "change-me-in-production";
    private int accessTokenExpiryMinutes = 30;
    private int refreshTokenExpiryDays = 7;
}
