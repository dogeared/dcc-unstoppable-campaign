package com.example.unstoppable.ui;

import com.example.unstoppable.domain.AccessRole;
import com.example.unstoppable.domain.CampaignStatus;
import com.example.unstoppable.service.CampaignNotFoundException;
import com.example.unstoppable.service.CampaignService;
import com.example.unstoppable.service.CurrentUser;
import com.example.unstoppable.service.InviteService;
import com.example.unstoppable.ui.component.AppFooter;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = UiTestConfig.class)
@ViewPackages(classes = {
        LandingView.class,
        LoginView.class,
        CampaignListView.class,
        NewCampaignView.class,
        JoinView.class,
        CampaignSheetView.class
})
class UiViewsTest extends SpringBrowserlessTest {

    @Autowired private CampaignService campaignService;
    @Autowired private InviteService inviteService;
    @Autowired private CurrentUser currentUser;
    @Autowired private AuthenticationContext authenticationContext;

    @BeforeEach
    void setUp() {
        reset(campaignService, inviteService, currentUser, authenticationContext);
        lenient().when(currentUser.id()).thenReturn("owner");
        lenient().when(currentUser.initials()).thenReturn("MS");
        lenient().when(currentUser.displayName()).thenReturn("Micah Silverman");
        lenient().when(currentUser.username()).thenReturn("micah");
    }

    @Test
    void loginViewRedirectsToKeycloak() {
        navigate(LoginView.class);

        assertTrue(getCurrentView() instanceof LoginView);
    }

    @Test
    void campaignListRendersEveryRoleAndStatus() {
        when(campaignService.listFor("owner")).thenReturn(List.of(
                UiFixtures.summary(AccessRole.OWNER, CampaignStatus.COMPLETED),
                UiFixtures.summary(AccessRole.EDITOR, CampaignStatus.ACTIVE),
                UiFixtures.summary(AccessRole.VIEWER, CampaignStatus.ACTIVE)));

        navigate(CampaignListView.class);

        assertEquals("Campaigns", find(H1.class).first().getText());
        assertEquals(3, find(Anchor.class).all().stream()
                .filter(anchor -> "Open sheet".equals(anchor.getText())).count());
        assertTrue(find(H2.class).all().isEmpty());
        assertTrue(find(com.vaadin.flow.component.html.Span.class).all().stream()
                .anyMatch(span -> "Owner".equals(span.getText())));
        assertTrue(find(com.vaadin.flow.component.html.Span.class).all().stream()
                .anyMatch(span -> "Editor".equals(span.getText())));
        assertTrue(find(com.vaadin.flow.component.html.Span.class).all().stream()
                .anyMatch(span -> "Viewer".equals(span.getText())));
        assertTrue(find(com.vaadin.flow.component.html.Span.class).all().stream()
                .anyMatch(span -> "Normal · complete".equals(span.getText())));
    }

    @Test
    void campaignListRendersEmptyState() {
        when(campaignService.listFor("owner")).thenReturn(List.of());

        navigate(CampaignListView.class);

        assertEquals("No campaigns yet.", find(H2.class).first().getText());
    }

    @Test
    void newCampaignEnablesCreateAndNavigates() {
        navigate(NewCampaignView.class);

        assertEquals("Start a campaign", find(H1.class).first().getText());
        TextField partyName = find(TextField.class).first();
        partyName.setValue("The Party");
        Button create = find(Button.class).first();
        assertTrue(create.isEnabled());

        UUID id = UUID.randomUUID();
        when(campaignService.create("owner", "The Party",
                com.example.unstoppable.domain.Difficulty.NORMAL)).thenReturn(id);
        test(create).click();

        verify(campaignService).create("owner", "The Party",
                com.example.unstoppable.domain.Difficulty.NORMAL);
    }

    @Test
    void newCampaignShowsErrorWhenCreateFails() {
        when(campaignService.create(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Party name is required"));

        navigate(NewCampaignView.class);
        find(TextField.class).first().setValue("The Party");
        test(find(Button.class).first()).click();

        assertTrue(find(com.vaadin.flow.component.notification.Notification.class)
                .all().stream().anyMatch(notification -> true));
    }

    @Test
    void joinViewAcceptsTheInvite() {
        UUID campaignId = UUID.randomUUID();
        when(inviteService.accept("token-1", "owner")).thenReturn(campaignId);

        navigate(JoinView.class, Map.of("token", "token-1"));

        verify(inviteService).accept("token-1", "owner");
    }

    @Test
    void joinViewShowsTheFailureMessage() {
        when(inviteService.accept("bad", "owner"))
                .thenThrow(new IllegalArgumentException("That invite link is not valid"));

        navigate(JoinView.class, Map.of("token", "bad"));

        assertEquals("That link did not work.", find(H1.class).first().getText());
    }

    @Test
    void sheetViewRendersAllSectionsForEditors() {
        when(campaignService.load(eq(UiFixtures.CAMPAIGN_ID), eq("owner")))
                .thenReturn(UiFixtures.richSheet(AccessRole.OWNER));

        navigate(CampaignSheetView.class,
                Map.of("campaignId", UiFixtures.CAMPAIGN_ID.toString()));

        assertEquals("The Brave", find(H1.class).first().getText());
        assertTrue(find(Anchor.class).all().stream()
                .anyMatch(anchor -> "Edit sheet".equals(anchor.getText())));
        assertTrue(find(com.vaadin.flow.component.html.Span.class).all().stream()
                .anyMatch(span -> "✓".equals(span.getText())));
        assertTrue(find(com.vaadin.flow.component.html.Span.class).all().stream()
                .anyMatch(span -> "eliminated".equals(span.getText())));
        assertTrue(find(com.vaadin.flow.component.html.Span.class).all().stream()
                .anyMatch(span -> "mastered".equals(span.getText())));
        assertTrue(find(com.vaadin.flow.component.html.Span.class).all().stream()
                .anyMatch(span -> "+ starting hand".equals(span.getText())));
        assertTrue(find(com.vaadin.flow.component.html.H3.class).all().stream()
                .anyMatch(heading -> "Floor 1".equals(heading.getText())));
    }

    @Test
    void sheetViewRendersEmptyLists() {
        when(campaignService.load(any(), any()))
                .thenReturn(UiFixtures.emptySheet(AccessRole.VIEWER));

        navigate(CampaignSheetView.class,
                Map.of("campaignId", UUID.randomUUID().toString()));

        assertTrue(find(Anchor.class).all().stream()
                .noneMatch(anchor -> "Edit sheet".equals(anchor.getText())));
        assertTrue(find(com.vaadin.flow.component.html.Paragraph.class).all().stream()
                .anyMatch(p -> "No achievements recorded yet.".equals(p.getText())));
        assertTrue(find(com.vaadin.flow.component.html.Paragraph.class).all().stream()
                .anyMatch(p -> "No permanent benefits purchased yet.".equals(p.getText())));
        assertTrue(find(com.vaadin.flow.component.html.Paragraph.class).all().stream()
                .anyMatch(p -> "No sessions logged yet. Add attempts from the editor."
                        .equals(p.getText())));
    }

    @Test
    void sheetViewShowsCompletedStatus() {
        when(campaignService.load(any(), any()))
                .thenReturn(UiFixtures.completedSheet(AccessRole.EDITOR));

        navigate(CampaignSheetView.class,
                Map.of("campaignId", UUID.randomUUID().toString()));

        assertTrue(find(com.vaadin.flow.component.html.Span.class).all().stream()
                .anyMatch(span -> "Complete".equals(span.getText())));
    }

    @Test
    void sheetViewHandlesMalformedLink() {
        navigate(CampaignSheetView.class, Map.of("campaignId", "not-a-uuid"));

        assertEquals("This sheet is not available.", find(H1.class).first().getText());
        assertEquals("The link is malformed.",
                find(com.vaadin.flow.component.html.Paragraph.class).first().getText());
    }

    @Test
    void sheetViewHandlesMissingCampaign() {
        when(campaignService.load(any(), any()))
                .thenThrow(new CampaignNotFoundException(UUID.randomUUID()));

        navigate(CampaignSheetView.class,
                Map.of("campaignId", UUID.randomUUID().toString()));

        assertEquals("This sheet is not available.", find(H1.class).first().getText());
    }

    @Test
    void sheetViewHandlesUnexpectedFailure() {
        when(campaignService.load(any(), any()))
                .thenThrow(new IllegalStateException("boom"));

        navigate(CampaignSheetView.class,
                Map.of("campaignId", UUID.randomUUID().toString()));

        assertEquals("This sheet could not be displayed.", find(H1.class).first().getText());
    }

    @Test
    void notificationsAreRaised() {
        Ui.success("Saved");
        Ui.error("Broken");

        assertTrue(find(com.vaadin.flow.component.notification.Notification.class)
                .all().size() >= 2);
    }

    @Test
    void mainLayoutShowsFooterWithVersion() {
        when(campaignService.listFor("owner")).thenReturn(List.of());

        navigate(CampaignListView.class);

        AppFooter footer = find(AppFooter.class).first();
        assertTrue(footer.getElement().getTextRecursively().contains("dogeared"));
        assertTrue(footer.getElement().getTextRecursively().contains("v"));
    }

    @Test
    void mainLayoutTogglesDrawerAndSignsOut() {        MainLayout layout = new MainLayout(currentUser, authenticationContext);
        com.vaadin.flow.component.UI.getCurrent().add(layout);

        NativeButton toggle = find(NativeButton.class, layout).all().stream()
                .filter(button -> "Toggle navigation".equals(
                        button.getElement().getAttribute("aria-label")))
                .findFirst().orElseThrow();
        test(toggle).click();

        NativeButton signOut = find(NativeButton.class, layout).all().stream()
                .filter(button -> "Sign out".equals(button.getText()))
                .findFirst().orElseThrow();
        test(signOut).click();

        verify(authenticationContext).logout();
    }
}
