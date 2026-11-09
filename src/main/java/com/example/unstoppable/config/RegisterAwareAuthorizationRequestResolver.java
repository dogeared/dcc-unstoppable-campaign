package com.example.unstoppable.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Wraps the default authorization request resolver so that
 * {@code /oauth2/authorization/keycloak?mode=register} opens Keycloak's
 * registration form directly instead of the sign-in form.
 *
 * <p>The dedicated {@code /protocol/openid-connect/registrations} endpoint is
 * used rather than the {@code prompt=create} parameter. Older Keycloak
 * releases ignore {@code prompt=create} and still render the sign-in form,
 * while the registration endpoint works across Keycloak versions and keeps
 * the usual state, nonce, and PKCE parameters intact.
 */
public class RegisterAwareAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String MODE_PARAMETER = "mode";
    private static final String REGISTER = "register";
    private static final String AUTH_PATH = "/protocol/openid-connect/auth";
    private static final String REGISTRATION_PATH = "/protocol/openid-connect/registrations";

    private final OAuth2AuthorizationRequestResolver delegate;

    public RegisterAwareAuthorizationRequestResolver(ClientRegistrationRepository repository) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                repository, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return withRegistrationEndpoint(delegate.resolve(request), request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return withRegistrationEndpoint(delegate.resolve(request, clientRegistrationId), request);
    }

    private static OAuth2AuthorizationRequest withRegistrationEndpoint(
            OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request) {
        if (authorizationRequest == null
                || !REGISTER.equals(request.getParameter(MODE_PARAMETER))) {
            return authorizationRequest;
        }
        String authorizationUri = authorizationRequest.getAuthorizationRequestUri();
        String registrationUri = authorizationUri.replace(AUTH_PATH, REGISTRATION_PATH);
        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .authorizationRequestUri(registrationUri)
                .build();
    }
}
