package com.example.unstoppable.service;

import com.example.unstoppable.domain.AccessRole;
import com.example.unstoppable.domain.Campaign;
import com.example.unstoppable.domain.CampaignMembership;
import com.example.unstoppable.domain.Difficulty;
import com.example.unstoppable.repository.CampaignMembershipRepository;
import com.example.unstoppable.repository.CampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignAccessServiceTest {

    @Mock private CampaignRepository campaigns;
    @Mock private CampaignMembershipRepository memberships;

    private CampaignAccessService service;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        service = new CampaignAccessService(campaigns, memberships);
        campaign = new Campaign("owner", "Party", Difficulty.NORMAL);
        lenient().when(campaigns.findById(campaign.getId())).thenReturn(Optional.of(campaign));
    }

    @Test
    void ownerHasEveryRight() {
        CampaignAccess access = service.requireReadable(campaign.getId(), "owner");

        assertThat(access.role()).isEqualTo(AccessRole.OWNER);
        assertThat(access.canEdit()).isTrue();
        assertThat(service.requireEditable(campaign.getId(), "owner").role())
                .isEqualTo(AccessRole.OWNER);
        assertThat(service.requireOwner(campaign.getId(), "owner").role())
                .isEqualTo(AccessRole.OWNER);
        assertThat(service.roleFor(campaign, "owner")).isEqualTo(AccessRole.OWNER);
    }

    @Test
    void editorCanReadAndEditButNotOwn() {
        CampaignMembership membership = new CampaignMembership(campaign, "editor",
                AccessRole.EDITOR);
        when(memberships.findByCampaignIdAndSubject(campaign.getId(), "editor"))
                .thenReturn(Optional.of(membership));

        assertThat(service.requireReadable(campaign.getId(), "editor").role())
                .isEqualTo(AccessRole.EDITOR);
        assertThat(service.requireEditable(campaign.getId(), "editor").canEdit()).isTrue();
        assertThatThrownBy(() -> service.requireOwner(campaign.getId(), "editor"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void viewerCanReadButNotEdit() {
        CampaignMembership membership = new CampaignMembership(campaign, "viewer",
                AccessRole.VIEWER);
        when(memberships.findByCampaignIdAndSubject(campaign.getId(), "viewer"))
                .thenReturn(Optional.of(membership));

        assertThat(service.requireReadable(campaign.getId(), "viewer").role())
                .isEqualTo(AccessRole.VIEWER);
        assertThatThrownBy(() -> service.requireEditable(campaign.getId(), "viewer"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.requireOwner(campaign.getId(), "viewer"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void strangerHasNoAccess() {
        when(memberships.findByCampaignIdAndSubject(campaign.getId(), "stranger"))
                .thenReturn(Optional.empty());

        assertThat(service.roleFor(campaign, "stranger")).isNull();
        assertThatThrownBy(() -> service.requireReadable(campaign.getId(), "stranger"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unknownCampaignIsNotFound() {
        UUID missing = UUID.randomUUID();
        when(campaigns.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireReadable(missing, "owner"))
                .isInstanceOf(CampaignNotFoundException.class)
                .hasMessageContaining(missing.toString());
    }

    @Test
    void campaignAccessWrapsRoleRules() {
        assertThat(new CampaignAccess(campaign, AccessRole.OWNER).canEdit()).isTrue();
        assertThat(new CampaignAccess(campaign, AccessRole.EDITOR).canEdit()).isTrue();
        assertThat(new CampaignAccess(campaign, AccessRole.VIEWER).canEdit()).isFalse();
    }

    @Test
    void membershipLookupUsesCampaignIdAndSubject() {
        when(memberships.findByCampaignIdAndSubject(any(), any())).thenReturn(Optional.empty());
        assertThat(service.roleFor(campaign, "nobody")).isNull();
    }
}
