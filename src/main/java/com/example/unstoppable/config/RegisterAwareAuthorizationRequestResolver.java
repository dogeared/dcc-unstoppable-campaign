package com.example.unstoppable.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Wraps the default authorization request resolver so that
 * {@code /oauth2/authorization/keycloak?mode=register} opens Keycloak's
 * registration form directly instead of the sign-in form.
 *
 * <p>Keycloak 26.1+ recognizes {@code prompt=create} on the authorization
 * endpoint and switches to the registration flow; older releases use
 * {@code kc_action=register}. Both are sent so either server version works,
 * and the flow stays a normal OIDC redirect without exposing Keycloak terms
 * in the application UI.
 */
public class RegisterAwareAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String MODE_PARAMETER = "mode";
    private static final String REGISTER = "register";
    private static final String KEYCLOAK_ACTION = "kc_action";

    private final OAuth2AuthorizationRequestResolver delegate;

    public RegisterAwareAuthorizationRequestResolver(ClientRegistrationRepository repository) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                repository, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return withRegistrationAction(delegate.resolve(request), request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return withRegistrationAction(delegate.resolve(request, clientRegistrationId), request);
    }

    private static OAuth2AuthorizationRequest withRegistrationAction(
            OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request) {
        if (authorizationRequest == null || !REGISTER.equals(request.getParameter(MODE_PARAMETER))) {
            return authorizationRequest;
        }
        Map<String, Object> additional = new HashMap<>(
                authorizationRequest.getAdditionalParameters());
        // Keycloak 26.1+ switches to the registration flow on prompt=create;
        // kc_action=register keeps older servers working (they ignore prompt).
        additional.put("prompt", "create");
        additional.put(KEYCLOAK_ACTION, REGISTER);
        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(additional)
                .build();
    }
}
