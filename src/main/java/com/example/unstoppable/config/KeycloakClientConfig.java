package com.example.unstoppable.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.Map;

/**
 * Builds the client registration explicitly instead of using OIDC discovery.
 * Discovery would bake the public Keycloak hostname into back-channel URLs,
 * which breaks when the app runs in Docker while the browser runs elsewhere.
 */
@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakClientConfig {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(KeycloakProperties properties) {
        ClientRegistration registration = ClientRegistration
                .withRegistrationId("keycloak")
                .clientName("Keycloak")
                .clientId(properties.clientId())
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri(properties.authorizationUri())
                .tokenUri(properties.tokenUri())
                .jwkSetUri(properties.jwkSetUri())
                .userInfoUri(properties.userInfoUri())
                .issuerUri(properties.issuer())
                .providerConfigurationMetadata(Map.of(
                        "end_session_endpoint", properties.endSessionEndpoint()))
                .userNameAttributeName("preferred_username")
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }
}
