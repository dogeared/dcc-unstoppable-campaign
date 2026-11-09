package com.example.unstoppable.service;

import com.example.unstoppable.domain.Difficulty;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CampaignTotalsTest {

    @Test
    void spendingReducesAvailableXpButNotFinalScore() {
        CampaignTotals totals = CampaignTotals.of(40, 0, 13, Difficulty.NORMAL);

        assertThat(totals.availableXp()).isEqualTo(27);
        assertThat(totals.finalScore()).isEqualTo(41);
    }

    @Test
    void defeatsReduceFinalScoreButNotAvailableXp() {
        CampaignTotals totals = CampaignTotals.of(40, 3, 0, Difficulty.HARD);

        assertThat(totals.availableXp()).isEqualTo(40);
        assertThat(totals.totalDefeats()).isEqualTo(3);
        assertThat(totals.finalScore()).isEqualTo(40 - 9 + 5);
    }

    @Test
    void difficultyBonusMatchesTheRulebook() {
        assertThat(CampaignTotals.of(0, 0, 0, Difficulty.NORMAL).difficultyBonus())
                .isEqualTo(1);
        assertThat(CampaignTotals.of(0, 0, 0, Difficulty.HARD).difficultyBonus())
                .isEqualTo(5);
        assertThat(CampaignTotals.of(0, 0, 0, Difficulty.EXPERT).difficultyBonus())
                .isEqualTo(10);
        assertThat(CampaignTotals.of(0, 0, 0, Difficulty.UNBEATABLE).difficultyBonus())
                .isEqualTo(20);
    }

    @Test
    void overspendingShowsNegativeAvailableXp() {
        CampaignTotals totals = CampaignTotals.of(10, 0, 14, Difficulty.NORMAL);

        assertThat(totals.availableXp()).isEqualTo(-4);
    }
}
