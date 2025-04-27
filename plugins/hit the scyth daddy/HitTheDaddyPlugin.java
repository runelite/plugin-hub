package com.hitthedaddy;

import net.runelite.api.Client;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.api.events.GameTick;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioInputStream;
import java.io.File;

@PluginDescriptor(
        name = "Hit The Daddy",
        description = "Plays a sound and shows an overlay when it's time to hit the boss.",
        tags = {"TOB", "boss", "helper"}
)
public class HitTheDaddyPlugin extends Plugin
{
    @Inject
    private Client client;

    @Override
    protected void startUp() throws Exception
    {
        // Plugin start logic (optional)
    }

    @Override
    protected void shutDown() throws Exception
    {
        // Plugin shutdown logic (optional)
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        // TODO: Replace with real detection logic
        // This will currently play every tick for TESTING
        playHitSound();
    }

    private void playHitSound()
    {
        try
        {
            File soundFile = new File("C:/Users/User/Desktop/hitmedaddy.wav"); // <--- Put your sound file here
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
