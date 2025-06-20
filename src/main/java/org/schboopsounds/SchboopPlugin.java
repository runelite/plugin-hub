package org.schboopsounds;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.ItemComposition;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.RuneLite;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.api.*;

import java.util.regex.Pattern;
import okhttp3.OkHttpClient;
import net.runelite.api.Skill;
import java.util.Timer;
import java.util.TimerTask;
import net.runelite.api.events.MenuOptionClicked;

import static net.runelite.api.Skill.*;


@Slf4j
@PluginDescriptor(
	name = "Schboop"
)
public class SchboopPlugin extends Plugin
{
	@Inject
	private ItemManager itemManager;
	private static final File CUSTOM_SOUNDS_DIR = new File(RuneLite.RUNELITE_DIR.getPath() + File.separator + "custom-sounds");
	private static final File SchboopMoo = new File(CUSTOM_SOUNDS_DIR, "moo.wav");
	private static final File WhaHappen = new File(CUSTOM_SOUNDS_DIR, "what_happened_long.wav");
	private static final File yellowv1 = new File(CUSTOM_SOUNDS_DIR, "yellow1.wav");
	private static final File yellowv2 = new File(CUSTOM_SOUNDS_DIR, "yellow2.wav");
	private static final File yellowv3 = new File(CUSTOM_SOUNDS_DIR, "yellow3.wav");
	private static final File yellowv5 = new File(CUSTOM_SOUNDS_DIR, "yellow5.wav");
	private static final File yellowv6 = new File(CUSTOM_SOUNDS_DIR, "yellow6.wav");
	private static final File pink = new File(CUSTOM_SOUNDS_DIR, "pink.wav");
	private static final File Dad1 = new File(CUSTOM_SOUNDS_DIR, "DadSnark1_v2.wav");
	private static final File Dad2 = new File(CUSTOM_SOUNDS_DIR, "DadSnark2.wav");
	private static final File MORON = new File(CUSTOM_SOUNDS_DIR, "MORON.wav");
	private static final File where = new File(CUSTOM_SOUNDS_DIR, "where.wav");
	private static final File troll1 = new File(CUSTOM_SOUNDS_DIR, "hydrate.wav");
	private static final File troll2 = new File(CUSTOM_SOUNDS_DIR, "deezballs.wav");
	private static final File troll3 = new File(CUSTOM_SOUNDS_DIR, "certified.wav");
	private static final File troll4 = new File(CUSTOM_SOUNDS_DIR, "beethoven.wav");
	private static final File troll5 = new File(CUSTOM_SOUNDS_DIR, "calm.wav");
	private static final File reading = new File(CUSTOM_SOUNDS_DIR, "4Nerds.wav");
	private static final File stroke = new File(CUSTOM_SOUNDS_DIR, "stroke.wav");
	private static final File piss_pool = new File(CUSTOM_SOUNDS_DIR, "piss_bowl.wav");
	private static final File laugh = new File(CUSTOM_SOUNDS_DIR, "Dad_Cackle.wav");
	private static final File ew = new File(CUSTOM_SOUNDS_DIR, "ew.wav");
	private static final File dragonhead = new File(CUSTOM_SOUNDS_DIR, "head.wav");
	private static final File[] SOUND_FILES = new File[]{
			SchboopMoo,
			WhaHappen,
			yellowv1,
			yellowv2,
			yellowv3,
			yellowv5,
			yellowv6,
			pink,
			Dad1,
			Dad2,
			MORON,
			where,
			troll1,
			troll2,
			troll3,
			troll4,
			troll5,
			stroke,
			reading,
			piss_pool,
			laugh,
			ew,
			dragonhead
	};

	// runelite haves the what_happened.wav file... need to convert to something it can stand

    //private List<String> highlightedItemsList = new CopyOnWriteArrayList<>();
	private static final long CLIP_TIME_UNLOADED = -2;

	private long lastClipTime = CLIP_TIME_UNLOADED;
	private Clip clip = null;

	@Inject
	private Client client;

	private int[] previousStats = new int[Skill.values().length];
	private boolean trackingPotion = false;

	@Inject
	private SchboopConfig config;

	@Inject
	private OkHttpClient okHttpClient;
	private static final Pattern COLLECTION_LOG_ITEM_REGEX = Pattern.compile(".*New item added to your collection log.*", Pattern.CASE_INSENSITIVE);
	//private static final Pattern COLLECTION_LOG_ITEM_REGEX = Pattern.compile("Test.*");
	private static final Pattern EASY_TASK_REGEX = Pattern.compile(".*Well done\\! You have completed an easy task.*", Pattern.CASE_INSENSITIVE);
	private static final Pattern MEDIUM_TASK_REGEX = Pattern.compile(".*Well done\\! You have completed a medium task.*", Pattern.CASE_INSENSITIVE);
	private static final Pattern HARD_TASK_REGEX = Pattern.compile(".*Well done\\! You have completed a hard task.*", Pattern.CASE_INSENSITIVE);
	private static final Pattern ELITE_TASK_REGEX = Pattern.compile(".*Well done\\! You have completed an elite task.*", Pattern.CASE_INSENSITIVE);
	private static final Pattern[] DIARY_LIST_REGEX = new Pattern[]{
			EASY_TASK_REGEX, MEDIUM_TASK_REGEX, HARD_TASK_REGEX, ELITE_TASK_REGEX
	};
	//private static final Pattern TASK_REGEX = Pattern.compile("Well done! You have completed.*");

	private static final Pattern NEW_LEVEL_REGEX = Pattern.compile("Congratulations, you've just advanced your.*");
	private static final Pattern LEVEL_69_REGEX = Pattern.compile(".*You are now level 69.*");
	private static final Pattern NEW_PLACE_REGEX = Pattern.compile("You have unlocked a new music track.*");
	private static final Pattern WEBFAIL_REGEX = Pattern.compile("Only a sharp blade can cut through this.*");
    //private static final Pattern POTION_REGEX = Pattern.compile(".*potion.*");

	private static final Pattern ROBERT_SPAM_REGEX = Pattern.compile(".*All hail robert prime.*");
	private static final Pattern ROBERT_SPAM_REGEX2 = Pattern.compile(".*I am a loyal bert.*");
	private static final Pattern ROBERT_SPAM_REGEX3 = Pattern.compile(".*Enter the light of robert prime.*");
	private static final Pattern ROBERT_SPAM_REGEX4 = Pattern.compile(".*Prime is love, prime is life.*");
	private static final Pattern ROBERT_SPAM_REGEX5 = Pattern.compile(".*I love robert prime.*");
	private static final Pattern STROKE_REGEX = Pattern.compile("You rub the.*");
// piss zone
	private static final Pattern DRINK_POOL = Pattern.compile("You feel reinvigorated after drinking from the pool.");
	
	private static final Pattern COW_EXAMINE_REGEX = Pattern.compile("Converts grass to.*");
	private static final Pattern COW_EXAMINE_REGEX2 = Pattern.compile("Beefy.*");
	private static final Pattern COW_EXAMINE_REGEX3 = Pattern.compile("Where beef comes from.*");
	private static final Pattern COW_EXAMINE_REGEX4 = Pattern.compile("A cow by any other name would smell as sweet.*");
	private static final Pattern DAIRY_EXAMINE_REGEX = Pattern.compile("Fit for milking.*");
	private static final Pattern CALF_EXAMINE_REGEX = Pattern.compile("Young and tender; nearly ready for the slaughter.*");
	private static final Pattern CALF_EXAMINE_REGEX2 = Pattern.compile("Prelude to a steak.*");
	private static final Pattern BOB_EXAMINE_REGEX = Pattern.compile("Hey, it's Bob the cat.*");
	private static final Pattern CAT_EXAMINE_REGEX = Pattern.compile("A fully grown feline.*");
	private static final Pattern KITTEN_EXAMINE_REGEX = Pattern.compile("A friendly little pet.*");
	private static final Pattern CHATMEOWUPPER = Pattern.compile(".*Meow.*");
	private static final Pattern CHATMEOWLOWER = Pattern.compile(".*meow.*");
	private static final Pattern[] MOO_LIST_REGEX = new Pattern[]{
			COW_EXAMINE_REGEX, COW_EXAMINE_REGEX2, COW_EXAMINE_REGEX3, COW_EXAMINE_REGEX4,
			DAIRY_EXAMINE_REGEX, CALF_EXAMINE_REGEX, BOB_EXAMINE_REGEX, CAT_EXAMINE_REGEX,
			KITTEN_EXAMINE_REGEX, CALF_EXAMINE_REGEX2, CHATMEOWUPPER, CHATMEOWLOWER
	};
	// bones you shouldn't bury:
	private static final Pattern BONES_REGEX1 = Pattern.compile(".*\\>Superior dragon bones\\<.*");
	private static final Pattern BONES_REGEX2 = Pattern.compile(".*\\>Wyrm bones\\<.*");
	private static final Pattern BONES_REGEX3 = Pattern.compile(".*\\>Wyvern bones\\<.*");
	private static final Pattern BONES_REGEX4 = Pattern.compile(".*\\>Raurg bones\\<.*");
	private static final Pattern BONES_REGEX5 = Pattern.compile(".*\\>Ourg bones\\<.*");
	private static final Pattern BONES_REGEX6 = Pattern.compile(".*\\>Lava dragon bones\\<.*");
	private static final Pattern BONES_REGEX7 = Pattern.compile(".*\\>Dragon bones\\<.*");
	private static final Pattern BONES_REGEX8 = Pattern.compile(".*\\>Dagannoth bones\\<.*");
	private static final Pattern BONES_REGEX10 = Pattern.compile(".*\\>Hydra bones\\<.*");
	private static final Pattern BONES_REGEX11 = Pattern.compile(".*\\>Fayrg bones\\<.*");
	private static final Pattern BONES_REGEX12 = Pattern.compile(".*\\>Drake bones\\<.*");
	private static final Pattern BONES_REGEX13 = Pattern.compile(".*\\>Babydragon bones\\<.*");
	// private static final Pattern BONES_REGEX14 = Pattern.compile(".*\\>Bones\\<.*"); //F2P test
	private static final Pattern[] BONES_LIST_REGEX = new Pattern[]{
			BONES_REGEX1, BONES_REGEX2, BONES_REGEX3, BONES_REGEX4,
			BONES_REGEX5, BONES_REGEX6, BONES_REGEX7, BONES_REGEX8,
			BONES_REGEX10, BONES_REGEX11, BONES_REGEX12, BONES_REGEX13//, BONES_REGEX14
	};
	// https://github.com/evaan/tedious-collection-log/blob/master/src/main/java/xyz/evaan/TediousCollectionLogPlugin.java
	// collection log related example -- this functionality may not work as I can't test it

	@Override
	protected void startUp() throws Exception
	{
		initSoundFiles();
		//playSound_Chaotic(stroke); // for testing purposes
		for (int i = 0; i < Skill.values().length; i++) // this keeps track of stats:
		{
			previousStats[i] = client.getBoostedSkillLevel(Skill.values()[i]);
		}
	}


	@Override
	protected void shutDown()
	{
		clip.close();
		clip = null;
	}

	@Subscribe
	public void onLootReceived(LootReceived lootReceived) {
		for (ItemStack stack : lootReceived.getItems()) {
			handleItem(stack.getId(), stack.getQuantity());
		}
	}


	@Subscribe
	public void onActorDeath(ActorDeath actorDeath) {
		Actor actor = actorDeath.getActor();

		if (!(actor instanceof Player)) {
			return;
		}

		Player player = (Player) actor;
		deathSound(player);
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged){
		if (statChanged.getSkill() != HITPOINTS)
		{
			if(statChanged.getSkill() != PRAYER){
				return;
			} else {
				float currentPray = client.getBoostedSkillLevel(PRAYER);
				int PRAYthresh50 = (int) ((float) client.getRealSkillLevel(HITPOINTS) * 0.4f);
				if (currentPray <= PRAYthresh50 && config.lowpray() && org.schboopsounds.counter.PRAYCOUNTER < 1)
				{
					playSound(pink);
					org.schboopsounds.counter.PRAYCOUNTER = 1;
				}
				if (currentPray > PRAYthresh50 && config.lowpray() && org.schboopsounds.counter.PRAYCOUNTER > 0)
				{
					org.schboopsounds.counter.PRAYCOUNTER = 0;
				}
			}
		} else {
			float currentHP = client.getBoostedSkillLevel(HITPOINTS);
			//int counter = 0;

			int HPthresh50 = (int) ((float) client.getRealSkillLevel(HITPOINTS) * 0.3f);
			int HPthresh40 = (int) ((float) client.getRealSkillLevel(HITPOINTS) * 0.24f );
			int HPthresh30 = (int) ((float) client.getRealSkillLevel(HITPOINTS) * 0.18f);
			int HPthresh20 = (int) ((float) client.getRealSkillLevel(HITPOINTS) * 0.12f);
			int HPthresh10 = (int) ((float) client.getRealSkillLevel(HITPOINTS) * 0.06f);

			//log.warn("Apparently the clip is playing");

			if (currentHP <= HPthresh50 && currentHP > HPthresh40 && config.lowHP() && org.schboopsounds.counter.HPCOUNTER < 1)
			{
				playSound(yellowv1);
				org.schboopsounds.counter.HPCOUNTER = 1;
			}
			if (currentHP <= HPthresh40 && currentHP > HPthresh30 && config.lowHP() && org.schboopsounds.counter.HPCOUNTER < 2)
			{
				playSound(yellowv2);
				org.schboopsounds.counter.HPCOUNTER = 2;
			}
			if (currentHP <= HPthresh30 && currentHP > HPthresh20 && config.lowHP() && org.schboopsounds.counter.HPCOUNTER < 3)
			{
				playSound(yellowv3);
				org.schboopsounds.counter.HPCOUNTER = 3;
			}
			if (currentHP <= HPthresh20 && currentHP > HPthresh10 && config.lowHP() && org.schboopsounds.counter.HPCOUNTER < 4)
			{
				playSound(yellowv5);
				org.schboopsounds.counter.HPCOUNTER = 4;
			}
			if (currentHP <= HPthresh10 && config.lowHP() && org.schboopsounds.counter.HPCOUNTER < 5)
			{
				playSound(yellowv6);
				org.schboopsounds.counter.HPCOUNTER = 5;
			}
			if (currentHP > HPthresh50 && config.lowHP() && org.schboopsounds.counter.HPCOUNTER > 0)
			{
				org.schboopsounds.counter.HPCOUNTER = 0;
			}
		}

	}


	@Subscribe
	public void onChatMessage(ChatMessage chatMessage) {
		if(config.Schboop_says_Moo()){
			for (Pattern pattern : MOO_LIST_REGEX) {
				if (pattern.matcher(chatMessage.getMessage()).matches()) {
					playSound(SchboopMoo);
					break; // Stop checking further once a match is found
				}
			}
		}

		// stroke because the doesn't work for whatever reason
		// WHY DIDN'T THIS WORK?????????
		if (STROKE_REGEX.matcher(chatMessage.getMessage()).matches()) {
			playSound_Chaotic(stroke);
		}

		// piss
		if (DRINK_POOL.matcher(chatMessage.getMessage()).matches() && config.all_hail_prime()) {
			playSound_Chaotic(piss_pool);
		}


		// secret chat triggers (cannot be toggled)
		if (ROBERT_SPAM_REGEX.matcher(chatMessage.getMessage()).matches() && chatMessage.getType() == ChatMessageType.PUBLICCHAT) {
			playSound_Chaotic(troll1);
		}
		if (ROBERT_SPAM_REGEX2.matcher(chatMessage.getMessage()).matches() && chatMessage.getType() == ChatMessageType.PUBLICCHAT) {
			playSound_Chaotic(troll2);
		}
		if (ROBERT_SPAM_REGEX3.matcher(chatMessage.getMessage()).matches() && chatMessage.getType() == ChatMessageType.PUBLICCHAT) {
			playSound_Chaotic(troll3);
		}
		if (ROBERT_SPAM_REGEX4.matcher(chatMessage.getMessage()).matches() && chatMessage.getType() == ChatMessageType.PUBLICCHAT) {
			playSound_Chaotic(troll4);
		}
		if (ROBERT_SPAM_REGEX5.matcher(chatMessage.getMessage()).matches() && chatMessage.getType() == ChatMessageType.PUBLICCHAT) {
			playSound_Chaotic(troll5);
		}
		// Dad makes fun of your achievements
		//if (chatMessage.getType() != ChatMessageType.GAMEMESSAGE && chatMessage.getType() != ChatMessageType.SPAM) {
		//	return;
		//}
		if(config.achievement()){
			for (Pattern pattern : DIARY_LIST_REGEX) {
				if (pattern.matcher(chatMessage.getMessage()).matches()) {
					playSound(Dad2);
					break; // Stop checking further once a match is found
				}
			}
		}
		if (COLLECTION_LOG_ITEM_REGEX.matcher(chatMessage.getMessage()).matches() && config.achievement()) {
			playSound(Dad2);
		}
		if (NEW_LEVEL_REGEX.matcher(chatMessage.getMessage()).matches() && config.achievement()) {
			if (LEVEL_69_REGEX.matcher(chatMessage.getMessage()).matches()) {
				playSound(Dad1); // plays if you level to 69
			} else {
				playSound(Dad1); // normal level up sound
			}
		}
		if (NEW_PLACE_REGEX.matcher(chatMessage.getMessage()).matches() && config.achievement()) {
			playSound(where); // replace this with a "where am I?" sound
		}
		// Pops calls you a moron for forgetting to bring a slash weapon to a web
		if (WEBFAIL_REGEX.matcher(chatMessage.getMessage()).matches() && config.roast() && chatMessage.getType() != ChatMessageType.SPAM) {
			playSound(MORON);
		}
	}

	// removed taking poison damage because I think it might be causing the bug
	//@Subscribe // Dad calls you a moron for taking poison damage
	//public void onHitsplatApplied(HitsplatApplied hitsplatApplied) {
	//	switch (hitsplatApplied.getHitsplat().getHitsplatType()) {
	//		case HitsplatID.POISON:
	//			if (client.getVarbitValue(Varbits.POISON) > 0 && config.roast()) { // Check poison status
	//				playSound(MORON);
	//			}
	//	}
	//}

	// Pops calls you a moron for burying expensive bones
	// Schboop tells you reading is for nerds
	// Pops calls you a moron for erroneous potion consumption (no stat changes)
	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		if("Bury".equals(event.getMenuOption()) && config.roast()) {
			for (Pattern pattern : BONES_LIST_REGEX) {
				if (pattern.matcher(event.getMenuTarget()).matches()) {
					playSound(MORON);
					break; // Stop checking further once a match is found
				}
			}
		}
		if("Read".equals(event.getMenuOption()) && config.roast()){
			// "Reading is for nerds" when you try to read something
			playSound(reading);
		}
		//if("Rub".equals(event.getMenuAction()) && config.roast()){
			 //Eva's idea :)
		//	playSound(stroke);
		//}
		if("Drink".equals(event.getMenuOption()) && config.roast()){
			String itemName = event.getMenuTarget().toLowerCase();
			if ((itemName.contains("potion") | itemName.contains("super ")) && !itemName.contains("stamina") && !itemName.contains("energy") && !itemName.contains("goading") && !itemName.contains("regeneration") && !itemName.contains("fire") && !itemName.contains("poison") && !itemName.contains("compost") && !itemName.contains(" set") && !itemName.contains(" kebab"))
			{
				// Store current stats before potion consumption
				for (int i = 0; i < Skill.values().length; i++)
				{
					previousStats[i] = client.getBoostedSkillLevel(Skill.values()[i]);
				}
				// Start tracking the potion use
				trackingPotion = true;

				// Schedule a check after a short delay (e.g., 1 second)
				new Timer().schedule(new TimerTask()
				{
					@Override
					public void run()
					{
						// Check if stats have changed after the potion use
						checkPotionEffect();
					}
				}, 1000);
			}
		}
	}


	private void checkPotionEffect()
	{
		if (!trackingPotion) return;
		int nStats = 0;
		// Compare the current stats with the stats before potion consumption
		for (int i = 0; i < Skill.values().length; i++)
		{
			int currentStat = client.getBoostedSkillLevel(Skill.values()[i]);

			if (currentStat != previousStats[i])
			{
				nStats = nStats+1;
				break;
			}
		}
		if(nStats == 0){
			playSound(MORON);
		}
		// Stop tracking potion use
		trackingPotion = false;
	}

	// This should fix Fursty's issue I hope
    private void handleItem(int id, int quantity) {
		final ItemComposition itemComposition = itemManager.getItemComposition(id);
		final String name = itemComposition.getName().toLowerCase();
		if (config.Schboop_says_Moo() && name.contains("raw beef")) {
			playSound_Chaotic(SchboopMoo);
		}
		if (name.contains("vorkath's head") | name.contains("elvarg's head") | name.contains("draconic visage")) {
			playSound_Chaotic(dragonhead);
		}
	}


	private void playSound(File f)
	{
		long currentTime = System.currentTimeMillis();
		if (clip == null || !clip.isOpen() || currentTime != lastClipTime) {
			lastClipTime = currentTime;
			try
			{
				// making sure last clip closes so we don't get multiple instances
				if (clip != null && clip.isOpen()) clip.close();

				AudioInputStream is = AudioSystem.getAudioInputStream(f);
				AudioFormat format = is.getFormat();
				DataLine.Info info = new DataLine.Info(Clip.class, format);
				clip = (Clip) AudioSystem.getLine(info);
				clip.open(is);
				setVolume(config.masterVolume());
				clip.start();
				//log.warn("Apparently the clip is playing");
			}
			catch (LineUnavailableException | UnsupportedAudioFileException | IOException e)
			{
				log.warn("Sound file error", e);
				lastClipTime = CLIP_TIME_UNLOADED;
			}
		}
	}

	private void playSound_Chaotic(File f)
	{
		long currentTime = System.currentTimeMillis();
		if (clip == null || !clip.isOpen() || currentTime != lastClipTime) {
			lastClipTime = currentTime;
			try
			{
				AudioInputStream is = AudioSystem.getAudioInputStream(f);
				AudioFormat format = is.getFormat();
				DataLine.Info info = new DataLine.Info(Clip.class, format);
				clip = (Clip) AudioSystem.getLine(info);
				clip.open(is);
				setVolume(config.masterVolume());
				clip.start();
				//log.warn("Apparently the clip is playing");
			}
			catch (LineUnavailableException | UnsupportedAudioFileException | IOException e)
			{
				log.warn("Sound file error", e);
				lastClipTime = CLIP_TIME_UNLOADED;
			}
		}
	}

	private void deathSound(Player player) {
		if (player == null) {
			return;
		}
		if (player == client.getLocalPlayer() && config.Pops_Died()) {
			playSound(WhaHappen);
		}
		if (player != client.getLocalPlayer()) {
			return;
		}

	}


	// sets volume using dB to linear conversion
	private void setVolume(int volume)
	{
		float vol = volume/100.0f;
		vol *= config.masterVolume()/100.0f;
		FloatControl gainControl = (FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
		gainControl.setValue(20.0f * (float) Math.log10(vol));
	}


	public class counter{
		public int HPcounter = 0;
	}

	@Provides
	SchboopConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SchboopConfig.class);
	}

	// initialize sound files if they haven't been created yet
	private void initSoundFiles()
	{
		if (!CUSTOM_SOUNDS_DIR.exists())
		{
			CUSTOM_SOUNDS_DIR.mkdirs();
		}

		for (File f : SOUND_FILES)
		{
			try
			{
				if (f.exists()) {
					continue;
				}
				InputStream stream = SchboopPlugin.class.getClassLoader().getResourceAsStream(f.getName());
				OutputStream out = new FileOutputStream(f);
				byte[] buffer = new byte[8 * 1024];
				int bytesRead;
				while ((bytesRead = stream.read(buffer)) != -1) {
					out.write(buffer, 0, bytesRead);
				}
				out.close();
				stream.close();
			}  catch (Exception e) {
				log.debug(e + ": " + f);
			}
		}
	}
}
