package com.example.unstoppable.domain;

/**
 * Campaign difficulty. The bonus is added to the final campaign score only.
 */
public enum Difficulty {
    NORMAL("Normal", 1),
    HARD("Hard", 5),
    EXPERT("Expert", 10),
    UNBEATABLE("Unbeatable", 20);

    private final String label;
    private final int scoreBonus;

    Difficulty(String label, int scoreBonus) {
        this.label = label;
        this.scoreBonus = scoreBonus;
    }

    public String label() {
        return label;
    }

    public int scoreBonus() {
        return scoreBonus;
    }
}
