package com.example.unstoppable.repository;

import com.example.unstoppable.domain.Benefit;
import com.example.unstoppable.domain.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {

    List<Purchase> findByCampaignIdOrderByPurchasedAtDesc(UUID campaignId);

    long countByParticipantIdAndBenefit(UUID participantId, Benefit benefit);
}
