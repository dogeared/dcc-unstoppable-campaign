package com.example.unstoppable.config;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers("/", "/login", "/join/**", "/actuator/health",
                        "/actuator/health/**", "/favicon.ico", "/styles/**",
                        "/images/**", "/VAADIN/**")
                .permitAll());

        http.oauth2Login(login -> login
                .authorizationEndpoint(endpoint -> endpoint
                        .authorizationRequestResolver(
                                new RegisterAwareAuthorizationRequestResolver(
                                        clientRegistrationRepository)))
                .defaultSuccessUrl("/campaigns", false));

        http.with(VaadinSecurityConfigurer.vaadin(), vaadin -> vaadin
                .oauth2LoginPage("/oauth2/authorization/keycloak", "{baseUrl}")
                .defaultSuccessUrl("/campaigns", false)
                .anyRequest(requests -> requests.authenticated()));

        return http.build();
    }
}
