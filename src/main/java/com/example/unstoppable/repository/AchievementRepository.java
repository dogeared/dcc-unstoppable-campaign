package com.example.unstoppable.repository;

import com.example.unstoppable.domain.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AchievementRepository extends JpaRepository<Achievement, UUID> {

    List<Achievement> findByCampaignIdOrderByFloorNumberAscNameAsc(UUID campaignId);

    Optional<Achievement> findByCampaignIdAndNameIgnoreCase(UUID campaignId, String name);

    @Query("select distinct a.name from Achievement a order by a.name")
    List<String> findDistinctNames();

    @Query("select distinct a.name from Achievement a where a.campaign.id = :campaignId order by a.name")
    List<String> findDistinctNamesForCampaign(@Param("campaignId") UUID campaignId);
}
