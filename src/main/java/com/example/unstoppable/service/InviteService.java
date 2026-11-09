package com.example.unstoppable.service;

import com.example.unstoppable.domain.AccessRole;
import com.example.unstoppable.domain.CampaignInvite;
import com.example.unstoppable.domain.CampaignMembership;
import com.example.unstoppable.repository.CampaignInviteRepository;
import com.example.unstoppable.repository.CampaignMembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InviteService {

    private final CampaignInviteRepository invites;
    private final CampaignMembershipRepository memberships;
    private final CampaignAccessService access;
    private final SecureRandom random = new SecureRandom();

    public InviteService(CampaignInviteRepository invites,
            CampaignMembershipRepository memberships,
            CampaignAccessService access) {
        this.invites = invites;
        this.memberships = memberships;
        this.access = access;
    }

    @Transactional
    public CreatedInvite create(UUID campaignId, String subject, AccessRole role,
            boolean publicAccess, int validDays) {
        var campaignAccess = access.requireOwner(campaignId, subject);
        requireGrantableRole(role);
        String token = newToken();
        Instant expiresAt = validDays > 0
                ? Instant.now().plus(validDays, ChronoUnit.DAYS)
                : null;
        CampaignInvite invite = invites.save(new CampaignInvite(
                campaignAccess.campaign(), hash(token), role, publicAccess,
                subject, expiresAt));
        return new CreatedInvite(token, view(invite));
    }

    /**
     * Changes an existing link's role and visibility. The token itself, and so
     * the URL, never changes. Editor links must stay private.
     */
    @Transactional
    public void update(UUID campaignId, String subject, UUID inviteId,
            AccessRole role, boolean publicAccess) {
        access.requireOwner(campaignId, subject);
        CampaignInvite invite = requireInvite(campaignId, inviteId);
        requireGrantableRole(role);
        invite.update(role, publicAccess);
    }

    /**
     * Resolves a link without granting access. Used by the join view to decide
     * whether an anonymous visitor may read the sheet in place.
     */
    @Transactional(readOnly = true)
    public Optional<InviteTarget> preview(String token) {
        return invites.findByTokenHash(hash(token))
                .filter(invite -> invite.isUsable(Instant.now()))
                .map(invite -> new InviteTarget(invite.getCampaign().getId(),
                        invite.getRole(), invite.isPublicAccess()));
    }

    @Transactional
    public List<InviteView> list(UUID campaignId, String subject) {
        access.requireOwner(campaignId, subject);
        return invites.findByCampaignIdOrderByCreatedAtDesc(campaignId).stream()
                .map(this::view)
                .toList();
    }

    @Transactional
    public void revoke(UUID campaignId, String subject, UUID inviteId) {
        access.requireOwner(campaignId, subject);
        requireInvite(campaignId, inviteId).revoke();
    }

    /**
     * Redeems an invite for the signed-in user. Reusable until revoked or expired.
     *
     * @return the campaign the user may now open
     */
    @Transactional
    public UUID accept(String token, String subject) {
        CampaignInvite invite = invites.findByTokenHash(hash(token))
                .orElseThrow(() -> new IllegalArgumentException("That invite link is not valid"));
        if (!invite.isUsable(Instant.now())) {
            throw new IllegalArgumentException("That invite link has expired or was revoked");
        }
        UUID campaignId = invite.getCampaign().getId();
        memberships.findByCampaignIdAndSubject(campaignId, subject)
                .ifPresentOrElse(
                        existing -> existing.elevate(invite.getRole()),
                        () -> memberships.save(new CampaignMembership(
                                invite.getCampaign(), subject, invite.getRole())));
        return campaignId;
    }

    private CampaignInvite requireInvite(UUID campaignId, UUID inviteId) {
        CampaignInvite invite = invites.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found"));
        if (!invite.getCampaign().getId().equals(campaignId)) {
            throw new IllegalArgumentException("Invite belongs to another campaign");
        }
        return invite;
    }

    private static void requireGrantableRole(AccessRole role) {
        if (role == AccessRole.OWNER) {
            throw new IllegalArgumentException("Invites cannot grant ownership");
        }
    }

    private InviteView view(CampaignInvite invite) {
        return new InviteView(invite.getId(), invite.getRole(), invite.isPublicAccess(),
                invite.getCreatedAt(), invite.getExpiresAt(), invite.getRevokedAt(),
                invite.isUsable(Instant.now()));
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record CreatedInvite(String token, InviteView view) {
    }

    /**
     * A usable link's destination without any membership change.
     */
    public record InviteTarget(UUID campaignId, AccessRole role, boolean publicAccess) {
    }
}
