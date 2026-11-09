package com.example.unstoppable.service;

import com.example.unstoppable.domain.AccessRole;
import com.example.unstoppable.domain.Campaign;

public record CampaignAccess(Campaign campaign, AccessRole role) {

    public boolean canEdit() {
        return role.canEdit();
    }
}
