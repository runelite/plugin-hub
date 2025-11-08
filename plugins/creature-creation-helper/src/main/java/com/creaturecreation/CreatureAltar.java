package com.creaturecreation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@Getter
public enum CreatureAltar
{
    SPIDINE("Spidine", "Red spider eggs + Raw sardine", "creaturecreation/spidine.png", WorldPoint.fromRegion(12100, 35, 10, 0)),
    JUBSTER("Jubster", "Raw jubbly + Raw lobster", "creaturecreation/jubster.png", WorldPoint.fromRegion(12100, 58, 29, 0)),
    NEWTROOST("Newtroost", "Eye of newt + Feather", "creaturecreation/newtroost.png", WorldPoint.fromRegion(12100, 51, 58, 0)),
    UNICOW("Unicow", "Cowhide + Unicorn horn", "creaturecreation/unicow.png", WorldPoint.fromRegion(12100, 11, 58, 0)),
    FROGEEL("Frogeel", "Raw cave eel + Giant frog legs", "creaturecreation/frogeel.png", WorldPoint.fromRegion(12100, 4, 29, 0)),
    SWORDCHICK("Swordchick", "Raw swordfish + Raw chicken", "creaturecreation/swordchick.png", WorldPoint.fromRegion(12100, 26, 10, 0));

    private final String creatureName;
    private final String ingredients;
    private final String imagePath;
    private final WorldPoint worldPoint;
    private BufferedImage cachedImage;

    CreatureAltar(String creatureName, String ingredients, String imagePath, WorldPoint worldPoint)
    {
        this.creatureName = creatureName;
        this.ingredients = ingredients;
        this.imagePath = imagePath;
        this.worldPoint = worldPoint;
    }

    public BufferedImage getImage()
    {
        if (cachedImage != null)
        {
            return cachedImage;
        }

        try
        {
            cachedImage = ImageIO.read(getResource(imagePath));
        }
        catch (IOException | IllegalArgumentException e)
        {
            cachedImage = null;
        }
        return cachedImage;
    }

    private java.net.URL getResource(String path)
    {
        return CreatureAltar.class.getClassLoader().getResource(path);
    }
}