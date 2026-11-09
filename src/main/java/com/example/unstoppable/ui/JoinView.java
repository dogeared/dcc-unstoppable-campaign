package com.example.unstoppable.ui;

import com.example.unstoppable.service.CampaignNotFoundException;
import com.example.unstoppable.service.CampaignService;
import com.example.unstoppable.service.CampaignSheet;
import com.example.unstoppable.service.CurrentUser;
import com.example.unstoppable.service.InviteService;
import com.example.unstoppable.ui.component.AppFooter;
import com.example.unstoppable.ui.component.CampaignSheetContent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;

import java.util.Optional;
import java.util.UUID;

@Route("join/:token")
@PageTitle("Join campaign")
@AnonymousAllowed
public class JoinView extends VerticalLayout implements BeforeEnterObserver {

    /** Mirrors Spring Security's private HttpSessionRequestCache attribute name. */
    private static final String SAVED_REQUEST = "SPRING_SECURITY_SAVED_REQUEST";

    private final InviteService inviteService;
    private final CampaignService campaignService;
    private final CurrentUser currentUser;

    public JoinView(InviteService inviteService, CampaignService campaignService,
            CurrentUser currentUser) {
        this.inviteService = inviteService;
        this.campaignService = campaignService;
        this.currentUser = currentUser;
        setPadding(false);
        setSpacing(false);
        addClassName("join-view");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        String token = event.getRouteParameters().get("token").orElse("");
        if (currentUser.isAuthenticated()) {
            accept(token, event);
            return;
        }
        anonymous(token, event);
    }

    private void accept(String token, BeforeEnterEvent event) {
        try {
            var campaignId = inviteService.accept(token, currentUser.id());
            event.forwardTo("campaigns/" + campaignId);
        } catch (RuntimeException exception) {
            renderProblem("That link did not work.", exception.getMessage(),
                    "Ask the campaign owner for a fresh invite link.");
        }
    }

    private void anonymous(String token, BeforeEnterEvent event) {
        Optional<InviteService.InviteTarget> target = inviteService.preview(token);
        if (target.isEmpty()) {
            renderProblem("That link did not work.",
                    "That invite link is not valid, has expired, or was revoked.",
                    "Ask the campaign owner for a fresh invite link.");
            return;
        }
        if (!target.get().publicAccess()) {
            redirectToLogin(event.getLocation());
            return;
        }
        renderPublic(target.get().campaignId());
    }

    private void renderPublic(UUID campaignId) {
        try {
            CampaignSheet sheet = campaignService.loadPublic(campaignId);
            removeClassName("join-view");
            addClassName("page");
            add(new CampaignSheetContent(sheet, inviteService, currentUser, true));
            add(new AppFooter());
        } catch (CampaignNotFoundException | AccessDeniedException exception) {
            renderProblem("This sheet is not available.",
                    "It may have been removed by its owner.", null);
        } catch (RuntimeException exception) {
            renderProblem("This sheet could not be displayed.",
                    "Something went wrong while loading the sheet. Please try again.", null);
        }
    }

    /**
     * Sends an anonymous visitor to Keycloak and saves the invite URL so the
     * OAuth success handler returns them here to accept the link. The current
     * servlet request is the Vaadin bootstrap/UIDL request, so the friendly
     * route location is saved instead of the request URI.
     */
    private void redirectToLogin(Location location) {
        saveReturnTo(location);
        UI.getCurrent().getPage().setLocation(LoginView.KEYCLOAK_LOGIN_URL);
    }

    private void saveReturnTo(Location location) {
        if (!(VaadinRequest.getCurrent() instanceof VaadinServletRequest current)) {
            return;
        }
        HttpServletRequest request = (HttpServletRequest) current.getRequest();
        String context = request.getContextPath() == null ? "" : request.getContextPath();
        String path = location.getPathWithQueryParameters();
        String requestUri = context + (path.startsWith("/") ? path : "/" + path);
        DefaultSavedRequest saved = new DefaultSavedRequest.Builder()
                .setRequestURI(requestUri)
                .setScheme(request.getScheme())
                .setServerName(request.getServerName())
                .setServerPort(request.getServerPort())
                .setMethod("GET")
                .build();
        request.getSession().setAttribute(SAVED_REQUEST, saved);
    }

    private void renderProblem(String title, String message, String hint) {
        removeAll();
        removeClassName("page");
        addClassName("join-view");
        Div card = new Div();
        card.addClassName("empty-card");
        card.add(new H1(title));
        card.add(new Paragraph(message));
        if (hint != null) {
            card.add(new Paragraph(hint));
        }
        add(card, new AppFooter());
    }
}
