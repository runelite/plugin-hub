package com.tobdeathsound;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;

@PluginDescriptor(
    name = "ToB Death Sound",
    description = "Plays a sound when a player nearby dies (only in ToB)",
    tags = {"death", "sound", "tob"}
)
public class ToBDeathSoundPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ToBDeathSoundConfig config;

    private boolean hasPlayed = false;

    @Override
    protected void configure()
    {
        bind(ToBDeathSoundConfig.class); // ✅ fixed this
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        hasPlayed = false;
    }

    @Subscribe
    public void onActorDeath(ActorDeath event)
    {
        if (!config.enabled() || hasPlayed || !isInTheatreOfBlood())
            return;

        if (event.getActor() instanceof Player)
        {
            playSound(config.soundChoice());
            hasPlayed = true;
        }
    }

    private boolean isInTheatreOfBlood()
    {
        int region = client.getLocalPlayer().getWorldLocation().getRegionID();
        return region == 14642 || region == 14643 || region == 14644 ||
               region == 13122 || region == 13123 || region == 13124 || region == 13125;
    }

    private void playSound(String fileName)
    {
        try (InputStream soundStream = getClass().getResourceAsStream("/" + fileName))
        {
            if (soundStream == null)
            {
                System.err.println("Could not find sound file: " + fileName);
                return;
            }

            try (AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundStream))
            {
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);

                FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float volumePercent = config.volume() / 100f;
                float dB = (float) (20.0 * Math.log10(Math.max(volumePercent, 0.01)));
                volume.setValue(dB);

                clip.start();
            }
        }
        catch (UnsupportedAudioFileException | IOException | LineUnavailableException | IllegalArgumentException e)
        {
            e.printStackTrace();
        }
    }
}
