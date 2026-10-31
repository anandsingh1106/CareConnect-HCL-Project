package com.careconnect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Typed view of the "careconnect.*" settings in application.yml. */
@ConfigurationProperties(prefix = "careconnect")
public record AppProperties(Jwt jwt, Cors cors, Storage storage, DemoData demoData) {

    public record Jwt(String secret, long accessTokenMinutes, long refreshTokenDays) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Storage(String uploadDir) {
    }

    public record DemoData(boolean enabled) {
    }
}
