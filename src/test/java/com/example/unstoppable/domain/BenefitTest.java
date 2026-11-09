package com.example.unstoppable.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BenefitTest {

    @Test
    void fixedCostsMatchTheSheet() {
        assertThat(Benefit.ELIMINATE_STARTING.cost(null)).isEqualTo(5);
        assertThat(Benefit.MASTER_LEVEL1.cost(null)).isEqualTo(4);
        assertThat(Benefit.MASTER_LEVEL2.cost(null)).isEqualTo(8);
        assertThat(Benefit.EXTRA_STARTING_HAND.cost(null)).isEqualTo(5);
    }

    @Test
    void variableCostsAddTheUpgradeGoldCost() {
        assertThat(Benefit.KEEP_STRIKE_UPGRADE.cost(0)).isEqualTo(1);
        assertThat(Benefit.KEEP_STRIKE_UPGRADE.cost(4)).isEqualTo(5);
        assertThat(Benefit.KEEP_CHARACTER_UPGRADE.cost(0)).isEqualTo(3);
        assertThat(Benefit.KEEP_CHARACTER_UPGRADE.cost(6)).isEqualTo(9);
    }

    @Test
    void variableCostRequiresX() {
        assertThatThrownBy(() -> Benefit.KEEP_STRIKE_UPGRADE.cost(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Benefit.KEEP_CHARACTER_UPGRADE.cost(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void perPlayerLimitsMatchTheCircles() {
        assertThat(Benefit.ELIMINATE_STARTING.perPlayerLimit()).isEqualTo(1);
        assertThat(Benefit.MASTER_LEVEL1.perPlayerLimit()).isEqualTo(1);
        assertThat(Benefit.MASTER_LEVEL2.perPlayerLimit()).isEqualTo(1);
        assertThat(Benefit.EXTRA_STARTING_HAND.perPlayerLimit()).isEqualTo(2);
        assertThat(Benefit.KEEP_STRIKE_UPGRADE.unlimited()).isTrue();
        assertThat(Benefit.KEEP_CHARACTER_UPGRADE.unlimited()).isTrue();
    }
}
