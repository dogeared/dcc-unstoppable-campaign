package com.example.unstoppable.service;

import com.example.unstoppable.domain.AccessRole;
import com.example.unstoppable.domain.Campaign;
import com.example.unstoppable.domain.CampaignInvite;
import com.example.unstoppable.domain.CampaignMembership;
import com.example.unstoppable.domain.Difficulty;
import com.example.unstoppable.repository.CampaignInviteRepository;
import com.example.unstoppable.repository.CampaignMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InviteServiceLifecycleTest {

    @Mock private CampaignInviteRepository invites;
    @Mock private CampaignMembershipRepository memberships;
    @Mock private CampaignAccessService access;

    private InviteService service;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        service = new InviteService(invites, memberships, access);
        campaign = new Campaign("owner", "Party", Difficulty.NORMAL);
        when(access.requireOwner(any(), any()))
                .thenAnswer(inv -> new CampaignAccess(campaign, AccessRole.OWNER));
        when(invites.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memberships.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createHashesTheTokenAndReturnsItOnce() {
        InviteService.CreatedInvite created = service.create(campaign.getId(), "owner",
                AccessRole.EDITOR, 7);

        assertThat(created.token()).isNotBlank();
        assertThat(created.view().role()).isEqualTo(AccessRole.EDITOR);
        assertThat(created.view().expiresAt()).isNotNull();
        assertThat(created.view().usable()).isTrue();

        ArgumentCaptor<CampaignInvite> captor = ArgumentCaptor.forClass(CampaignInvite.class);
        verify(invites).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isEqualTo(InviteService.hash(created.token()));
        assertThat(captor.getValue().getTokenHash()).doesNotContain(created.token());
        assertThat(captor.getValue().getCreatedBySub()).isEqualTo("owner");
    }

    @Test
    void createWithoutExpiryAndWithoutOwnership() {
        InviteService.CreatedInvite created = service.create(campaign.getId(), "owner",
                AccessRole.VIEWER, 0);

        assertThat(created.view().expiresAt()).isNull();
        assertThatThrownBy(() -> service.create(campaign.getId(), "owner",
                AccessRole.OWNER, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ownership");
        verify(invites, times(1)).save(any());
    }

    @Test
    void listMapsInvitesAndRequiresOwner() {
        CampaignInvite invite = new CampaignInvite(campaign, "hash", AccessRole.VIEWER,
                "owner", null);
        when(invites.findByCampaignIdOrderByCreatedAtDesc(campaign.getId()))
                .thenReturn(List.of(invite));

        List<InviteView> views = service.list(campaign.getId(), "owner");

        assertThat(views).hasSize(1);
        assertThat(views.getFirst().id()).isEqualTo(invite.getId());
        assertThat(views.getFirst().usable()).isTrue();
    }

    @Test
    void revokeValidatesCampaignAndRevokes() {
        CampaignInvite invite = new CampaignInvite(campaign, "hash", AccessRole.VIEWER,
                "owner", null);
        when(invites.findById(invite.getId())).thenReturn(Optional.of(invite));

        service.revoke(campaign.getId(), "owner", invite.getId());

        assertThat(invite.getRevokedAt()).isNotNull();
        assertThatThrownBy(() -> service.revoke(campaign.getId(), "owner", UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void revokeRejectsInviteFromAnotherCampaign() {
        Campaign other = new Campaign("other", "Other", Difficulty.NORMAL);
        CampaignInvite invite = new CampaignInvite(other, "hash", AccessRole.VIEWER,
                "owner", null);
        when(invites.findById(invite.getId())).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> service.revoke(campaign.getId(), "owner", invite.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("another campaign");
    }

    @Test
    void acceptCreatesMembershipForNewUser() {
        CampaignInvite invite = new CampaignInvite(campaign, "hash", AccessRole.EDITOR,
                "owner", null);
        when(invites.findByTokenHash(InviteService.hash("hash"))).thenReturn(Optional.of(invite));
        when(memberships.findByCampaignIdAndSubject(campaign.getId(), "subject"))
                .thenReturn(Optional.empty());

        UUID campaignId = service.accept("hash", "subject");

        assertThat(campaignId).isEqualTo(campaign.getId());
        ArgumentCaptor<CampaignMembership> captor =
                ArgumentCaptor.forClass(CampaignMembership.class);
        verify(memberships).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(AccessRole.EDITOR);
    }

    @Test
    void acceptElevatesAnExistingViewer() {
        CampaignInvite invite = new CampaignInvite(campaign, "hash", AccessRole.EDITOR,
                "owner", null);
        CampaignMembership membership = new CampaignMembership(campaign, "subject",
                AccessRole.VIEWER);
        when(invites.findByTokenHash(InviteService.hash("hash"))).thenReturn(Optional.of(invite));
        when(memberships.findByCampaignIdAndSubject(campaign.getId(), "subject"))
                .thenReturn(Optional.of(membership));

        service.accept("hash", "subject");

        assertThat(membership.getRole()).isEqualTo(AccessRole.EDITOR);
        verify(memberships, never()).save(any());
    }

    @Test
    void acceptRejectsUnknownExpiredAndRevokedInvites() {
        when(invites.findByTokenHash("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.accept("missing", "subject"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not valid");

        CampaignInvite expired = new CampaignInvite(campaign, "expired", AccessRole.EDITOR,
                "owner", Instant.now().minusSeconds(60));
        when(invites.findByTokenHash(InviteService.hash("expired"))).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.accept("expired", "subject"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");

        CampaignInvite revoked = new CampaignInvite(campaign, "revoked", AccessRole.EDITOR,
                "owner", null);
        revoked.revoke();
        when(invites.findByTokenHash(InviteService.hash("revoked"))).thenReturn(Optional.of(revoked));
        assertThatThrownBy(() -> service.accept("revoked", "subject"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revoked");
    }
}
