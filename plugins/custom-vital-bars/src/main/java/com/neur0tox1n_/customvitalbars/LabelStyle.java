package com.neur0tox1n_.customvitalbars;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LabelStyle
{
    SHOW_CURRENT_AND_MAXIMUM("CURRENT / MAXIMUM"),
    SHOW_CURRENT("CURRENT"),
    SHOW_PERCENTAGE("CURRENT %"),
    HIDE("Hide label");

    private final String name;

    @Override
    public String toString()
    {
        return getName();
    }
}
