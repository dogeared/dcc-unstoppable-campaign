package com.example.unstoppable.ui;

import com.example.unstoppable.service.CampaignService;
import com.example.unstoppable.service.CurrentUser;
import com.example.unstoppable.service.InviteService;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Reduced Spring context for browserless Vaadin tests: the UI components are
 * real, the services and the current user are mocks.
 */
@Configuration
@ComponentScan(basePackages = "com.example.unstoppable.ui")
public class UiTestConfig {

    @Bean
    public CampaignService campaignService() {
        return Mockito.mock(CampaignService.class);
    }

    @Bean
    public InviteService inviteService() {
        return Mockito.mock(InviteService.class);
    }

    @Bean
    public CurrentUser currentUser() {
        return Mockito.mock(CurrentUser.class);
    }

    @Bean
    public AuthenticationContext authenticationContext() {
        return Mockito.mock(AuthenticationContext.class);
    }
}
