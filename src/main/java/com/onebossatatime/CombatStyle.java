package com.onebossatatime;

public enum CombatStyle
{
    MELEE("Melee"),
    RANGED("Ranged"),
    MAGIC("Magic");

    private final String label;

    CombatStyle(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return label;
    }
}
