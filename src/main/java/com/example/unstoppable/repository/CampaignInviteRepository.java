package com.example.unstoppable.repository;

import com.example.unstoppable.domain.CampaignInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignInviteRepository extends JpaRepository<CampaignInvite, UUID> {

    Optional<CampaignInvite> findByTokenHash(String tokenHash);

    List<CampaignInvite> findByCampaignIdOrderByCreatedAtDesc(UUID campaignId);
}
