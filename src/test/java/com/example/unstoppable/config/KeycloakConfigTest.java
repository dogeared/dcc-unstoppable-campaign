package com.example.unstoppable.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakConfigTest {

    private final KeycloakProperties properties = new KeycloakProperties(
            "https://auth.example.test/", "http://keycloak:8080/", "unstoppable", "app");

    @Test
    void propertiesBuildEveryEndpointAndTrimSlashes() {
        assertThat(properties.publicUrl()).isEqualTo("https://auth.example.test");
        assertThat(properties.internalUrl()).isEqualTo("http://keycloak:8080");
        assertThat(properties.issuer()).isEqualTo("https://auth.example.test/realms/unstoppable");
        assertThat(properties.authorizationUri())
                .isEqualTo("https://auth.example.test/realms/unstoppable/protocol/openid-connect/auth");
        assertThat(properties.endSessionEndpoint())
                .isEqualTo("https://auth.example.test/realms/unstoppable/protocol/openid-connect/logout");
        assertThat(properties.tokenUri())
                .isEqualTo("http://keycloak:8080/realms/unstoppable/protocol/openid-connect/token");
        assertThat(properties.jwkSetUri())
                .isEqualTo("http://keycloak:8080/realms/unstoppable/protocol/openid-connect/certs");
        assertThat(properties.userInfoUri())
                .isEqualTo("http://keycloak:8080/realms/unstoppable/protocol/openid-connect/userinfo");
    }

    @Test
    void propertiesTolerateNullUrls() {
        KeycloakProperties blank = new KeycloakProperties(null, null, "realm", "app");
        assertThat(blank.publicUrl()).isNull();
        assertThat(blank.internalUrl()).isNull();
    }

    @Test
    void clientRegistrationIsBuiltFromProperties() {
        ClientRegistrationRepository repository = new KeycloakClientConfig()
                .clientRegistrationRepository(properties);

        ClientRegistration registration = repository.findByRegistrationId("keycloak");

        assertThat(registration).isNotNull();
        assertThat(registration.getClientId()).isEqualTo("app");
        assertThat(registration.getClientName()).isEqualTo("Keycloak");
        assertThat(registration.getClientAuthenticationMethod())
                .isEqualTo(ClientAuthenticationMethod.NONE);
        assertThat(registration.getAuthorizationGrantType())
                .isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
        assertThat(registration.getRedirectUri())
                .isEqualTo("{baseUrl}/login/oauth2/code/{registrationId}");
        assertThat(registration.getScopes()).containsExactlyInAnyOrder("openid", "profile", "email");
        assertThat(registration.getProviderDetails().getAuthorizationUri())
                .isEqualTo(properties.authorizationUri());
        assertThat(registration.getProviderDetails().getTokenUri())
                .isEqualTo(properties.tokenUri());
        assertThat(registration.getProviderDetails().getJwkSetUri())
                .isEqualTo(properties.jwkSetUri());
        assertThat(registration.getProviderDetails().getIssuerUri())
                .isEqualTo(properties.issuer());
        assertThat(registration.getProviderDetails().getUserInfoEndpoint().getUri())
                .isEqualTo(properties.userInfoUri());
        assertThat(registration.getProviderDetails().getConfigurationMetadata())
                .containsEntry("end_session_endpoint", properties.endSessionEndpoint());
        assertThat(registration.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName())
                .isEqualTo("preferred_username");
    }
}
