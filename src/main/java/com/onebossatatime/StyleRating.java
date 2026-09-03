package com.onebossatatime;

public enum StyleRating
{
    BEST("BEST"),
    GOOD("GOOD"),
    SITUATIONAL("SITUATIONAL"),
    POOR("POOR");

    private final String label;

    StyleRating(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return label;
    }
}
