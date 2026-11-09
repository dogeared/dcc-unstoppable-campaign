package com.example.unstoppable.service;

import com.vaadin.flow.spring.security.AuthenticationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserTest {

    @Mock private AuthenticationContext authenticationContext;
    @Mock private OidcUser oidcUser;

    private CurrentUser currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new CurrentUser(authenticationContext);
    }

    @Test
    void exposesIdentityFromTheOidcUser() {
        when(authenticationContext.getAuthenticatedUser(OidcUser.class))
                .thenReturn(Optional.of(oidcUser));
        when(oidcUser.getSubject()).thenReturn("subject-1");
        when(oidcUser.getFullName()).thenReturn("Micah Silverman");
        when(oidcUser.getPreferredUsername()).thenReturn("micah");

        assertThat(currentUser.isAuthenticated()).isTrue();
        assertThat(currentUser.id()).isEqualTo("subject-1");
        assertThat(currentUser.displayName()).isEqualTo("Micah Silverman");
        assertThat(currentUser.username()).isEqualTo("micah");
        assertThat(currentUser.initials()).isEqualTo("MS");
    }

    @Test
    void fallsBackToUsernameWhenFullNameIsMissing() {
        when(authenticationContext.getAuthenticatedUser(OidcUser.class))
                .thenReturn(Optional.of(oidcUser));
        when(oidcUser.getFullName()).thenReturn(" ");
        when(oidcUser.getPreferredUsername()).thenReturn("crawler");

        assertThat(currentUser.displayName()).isEqualTo("crawler");
        assertThat(currentUser.initials()).isEqualTo("C");
    }

    @Test
    void handlesAnonymousUser() {
        when(authenticationContext.getAuthenticatedUser(OidcUser.class))
                .thenReturn(Optional.empty());

        assertThat(currentUser.isAuthenticated()).isFalse();
        assertThat(currentUser.displayName()).isEqualTo("Guest");
        assertThat(currentUser.username()).isEmpty();
        assertThat(currentUser.initials()).isEqualTo("G");
        assertThatThrownBy(currentUser::id)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void initialsUseFirstAndLastWord() {
        when(authenticationContext.getAuthenticatedUser(OidcUser.class))
                .thenReturn(Optional.of(oidcUser));
        when(oidcUser.getFullName()).thenReturn("carl  the  crawler");

        assertThat(currentUser.initials()).isEqualTo("CC");
    }

    @Test
    void initialsFallBackToQuestionMarkForBlankName() {
        when(authenticationContext.getAuthenticatedUser(OidcUser.class))
                .thenReturn(Optional.of(oidcUser));
        when(oidcUser.getFullName()).thenReturn("");
        when(oidcUser.getPreferredUsername()).thenReturn("");

        assertThat(currentUser.initials()).isEqualTo("?");
    }
}
