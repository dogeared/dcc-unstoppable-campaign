package com.example.unstoppable.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class SecurityConfigTest {

    @Test
    void securityFilterChainIsBuiltWithOauthLoginAndVaadin() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class,
                withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        ClientRegistrationRepository repository =
                mock(ClientRegistrationRepository.class);
        DefaultSecurityFilterChain chain = mock(DefaultSecurityFilterChain.class);
        when(http.build()).thenReturn(chain);

        SecurityFilterChain result = new SecurityConfig()
                .securityFilterChain(http, repository);

        assertThat(result).isSameAs(chain);
        verify(http).authorizeHttpRequests(any());
        verify(http).oauth2Login(any());
        verify(http).with(any(), any());
    }
}
