package com.example.unstoppable.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Keycloak connection details. The public URL is what the browser sees and
 * therefore the token issuer; the internal URL is used for back-channel calls
 * (token exchange and JWKS) and may differ inside Docker.
 */
@ConfigurationProperties(prefix = "app.keycloak")
public record KeycloakProperties(
        String publicUrl,
        String internalUrl,
        String realm,
        String clientId) {

    public KeycloakProperties {
        publicUrl = trimTrailingSlash(publicUrl);
        internalUrl = trimTrailingSlash(internalUrl);
    }

    public String issuer() {
        return publicUrl + "/realms/" + realm;
    }

    public String authorizationUri() {
        return issuer() + "/protocol/openid-connect/auth";
    }

    public String endSessionEndpoint() {
        return issuer() + "/protocol/openid-connect/logout";
    }

    public String tokenUri() {
        return internalUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public String jwkSetUri() {
        return internalUrl + "/realms/" + realm + "/protocol/openid-connect/certs";
    }

    public String userInfoUri() {
        return internalUrl + "/realms/" + realm + "/protocol/openid-connect/userinfo";
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) {
            return null;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
