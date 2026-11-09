package com.example.unstoppable.repository;

import com.example.unstoppable.domain.CampaignMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignMembershipRepository extends JpaRepository<CampaignMembership, UUID> {

    List<CampaignMembership> findBySubject(String subject);

    Optional<CampaignMembership> findByCampaignIdAndSubject(UUID campaignId, String subject);
}
