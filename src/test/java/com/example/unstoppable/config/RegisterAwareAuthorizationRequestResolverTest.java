package com.example.unstoppable.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterAwareAuthorizationRequestResolverTest {

    private static final ClientRegistration REGISTRATION = ClientRegistration
            .withRegistrationId("keycloak")
            .clientId("app")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid")
            .authorizationUri("https://auth.example.test/realms/unstoppable/protocol/openid-connect/auth")
            .tokenUri("https://auth.example.test/realms/unstoppable/protocol/openid-connect/token")
            .jwkSetUri("https://auth.example.test/realms/unstoppable/protocol/openid-connect/certs")
            .build();

    private final ClientRegistrationRepository repository =
            new InMemoryClientRegistrationRepository(REGISTRATION);
    private final RegisterAwareAuthorizationRequestResolver resolver =
            new RegisterAwareAuthorizationRequestResolver(repository);

    private static MockHttpServletRequest request(String mode) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/oauth2/authorization/keycloak");
        request.setServerName("app.example.test");
        request.setScheme("https");
        request.setServerPort(443);
        if (mode != null) {
            request.setParameter("mode", mode);
        }
        return request;
    }

    @Test
    void registerModeAddsBothRegistrationParameters() {
        OAuth2AuthorizationRequest resolved = resolver.resolve(request("register"));

        assertThat(resolved).isNotNull();
        assertThat(resolved.getAdditionalParameters())
                .containsEntry("prompt", "create")
                .containsEntry("kc_action", "register");
    }

    @Test
    void registerModeWorksWithExplicitRegistrationId() {
        OAuth2AuthorizationRequest resolved = resolver.resolve(request("register"), "keycloak");

        assertThat(resolved).isNotNull();
        assertThat(resolved.getAdditionalParameters())
                .containsEntry("prompt", "create")
                .containsEntry("kc_action", "register");
    }

    @Test
    void otherModesKeepTheDefaultRequest() {
        OAuth2AuthorizationRequest resolved = resolver.resolve(request(null));

        assertThat(resolved).isNotNull();
        assertThat(resolved.getAdditionalParameters()).doesNotContainKeys("prompt", "kc_action");

        OAuth2AuthorizationRequest login = resolver.resolve(request("login"));
        assertThat(login.getAdditionalParameters()).doesNotContainKeys("prompt", "kc_action");
    }

    @Test
    void unknownRegistrationIsRejected() {
        assertThatThrownBy(() -> resolver.resolve(request("register"), "missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing");
    }
}
