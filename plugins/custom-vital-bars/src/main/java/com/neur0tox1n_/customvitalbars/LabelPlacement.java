package com.neur0tox1n_.customvitalbars;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LabelPlacement
{
    TOP("Top"),
    BOTTOM("Bottom"),
    LEFT("Left"),
    RIGHT("Right"),
    CENTRE("Centre");

    private final String name;

    @Override
    public String toString()
    {
        return getName();
    }
}
