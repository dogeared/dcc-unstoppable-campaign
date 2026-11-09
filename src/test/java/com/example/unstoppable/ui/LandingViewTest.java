package com.example.unstoppable.ui;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Anchor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = UiTestConfig.class)
@ViewPackages(classes = LandingView.class)
class LandingViewTest extends SpringBrowserlessTest {

    @Test
    void anonymousVisitorSeesLoginAndRegister() {
        navigate(LandingView.class);

        List<Anchor> anchors = find(Anchor.class).all();
        assertEquals(List.of("Log in", "Register"),
                anchors.stream().map(Anchor::getText).toList());
        assertEquals("/oauth2/authorization/keycloak", anchors.getFirst().getHref());
        assertEquals("/oauth2/authorization/keycloak?mode=register",
                anchors.get(1).getHref());
    }
}
