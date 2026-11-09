package com.example.unstoppable.service;

import com.example.unstoppable.domain.Difficulty;

/**
 * Derived campaign totals. Spending XP never changes the final campaign score;
 * defeats never change the spendable XP total.
 */
public record CampaignTotals(
        int floorScoreTotal,
        int totalDefeats,
        int spentXp,
        int availableXp,
        int difficultyBonus,
        int finalScore) {

    public static CampaignTotals of(int floorScoreTotal, int totalDefeats,
            int spentXp, Difficulty difficulty) {
        int availableXp = floorScoreTotal - spentXp;
        int finalScore = floorScoreTotal - (3 * totalDefeats)
                + difficulty.scoreBonus();
        return new CampaignTotals(floorScoreTotal, totalDefeats, spentXp,
                availableXp, difficulty.scoreBonus(), finalScore);
    }
}
