package com.example.unstoppable.service;

import com.example.unstoppable.domain.AccessRole;
import com.example.unstoppable.domain.Campaign;
import com.example.unstoppable.repository.CampaignMembershipRepository;
import com.example.unstoppable.repository.CampaignRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CampaignAccessService {

    private final CampaignRepository campaigns;
    private final CampaignMembershipRepository memberships;

    public CampaignAccessService(CampaignRepository campaigns,
            CampaignMembershipRepository memberships) {
        this.campaigns = campaigns;
        this.memberships = memberships;
    }

    @Transactional(readOnly = true)
    public CampaignAccess requireReadable(UUID campaignId, String subject) {
        Campaign campaign = find(campaignId);
        AccessRole role = roleFor(campaign, subject);
        if (role == null) {
            throw new AccessDeniedException("You do not have access to this campaign");
        }
        return new CampaignAccess(campaign, role);
    }

    @Transactional(readOnly = true)
    public CampaignAccess requireEditable(UUID campaignId, String subject) {
        CampaignAccess access = requireReadable(campaignId, subject);
        if (!access.canEdit()) {
            throw new AccessDeniedException("You can view but not edit this campaign");
        }
        return access;
    }

    @Transactional(readOnly = true)
    public CampaignAccess requireOwner(UUID campaignId, String subject) {
        CampaignAccess access = requireReadable(campaignId, subject);
        if (access.role() != AccessRole.OWNER) {
            throw new AccessDeniedException("Only the campaign owner can do that");
        }
        return access;
    }

    @Transactional(readOnly = true)
    public AccessRole roleFor(Campaign campaign, String subject) {
        if (campaign.getOwnerSub().equals(subject)) {
            return AccessRole.OWNER;
        }
        return memberships.findByCampaignIdAndSubject(campaign.getId(), subject)
                .map(com.example.unstoppable.domain.CampaignMembership::getRole)
                .orElse(null);
    }

    private Campaign find(UUID campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new CampaignNotFoundException(campaignId));
    }
}
