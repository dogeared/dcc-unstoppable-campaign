package com.example.unstoppable.domain;

/**
 * The six permanent benefits on Side B of the campaign sheet.
 */
public enum Benefit {
    ELIMINATE_STARTING("Eliminate a starting card", 5, 1, false, true),
    MASTER_LEVEL1("Master a level 1 card", 4, 1, false, false),
    MASTER_LEVEL2("Master a level 2 card", 8, 1, false, false),
    EXTRA_STARTING_HAND("Extra starting-hand card", 5, 2, false, true),
    KEEP_STRIKE_UPGRADE("Keep a Strike / New Achievement! / Rest upgrade", 0, 0, true, true),
    KEEP_CHARACTER_UPGRADE("Keep a character or mastered card upgrade", 0, 0, true, true);

    private final String label;
    private final int baseCost;
    private final int perPlayerLimit;
    private final boolean variableCost;
    private final boolean requiresDeckSlot;

    Benefit(String label, int baseCost, int perPlayerLimit,
            boolean variableCost, boolean requiresDeckSlot) {
        this.label = label;
        this.baseCost = baseCost;
        this.perPlayerLimit = perPlayerLimit;
        this.variableCost = variableCost;
        this.requiresDeckSlot = requiresDeckSlot;
    }

    public String label() {
        return label;
    }

    public int perPlayerLimit() {
        return perPlayerLimit;
    }

    public boolean unlimited() {
        return perPlayerLimit == 0;
    }

    public boolean variableCost() {
        return variableCost;
    }

    public boolean requiresDeckSlot() {
        return requiresDeckSlot;
    }

    /**
     * @param xValue the gold cost of the upgrade for the two X-based benefits
     */
    public int cost(Integer xValue) {
        return switch (this) {
            case KEEP_STRIKE_UPGRADE -> requireX(xValue) + 1;
            case KEEP_CHARACTER_UPGRADE -> requireX(xValue) + 3;
            default -> baseCost;
        };
    }

    public String costLabel() {
        return switch (this) {
            case KEEP_STRIKE_UPGRADE -> "X + 1";
            case KEEP_CHARACTER_UPGRADE -> "X + 3";
            default -> Integer.toString(baseCost);
        };
    }

    private static int requireX(Integer xValue) {
        if (xValue == null || xValue < 0) {
            throw new IllegalArgumentException("X must be zero or greater");
        }
        return xValue;
    }
}
