package com.onebossatatime;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class OneBossAtATimePanel extends PluginPanel
{
    private final OneBossAtATimePlugin plugin;
    private final GearAdvisor gearAdvisor;
    private final UpgradeAdvisor upgradeAdvisor;

    private final JLabel progressLabel = new JLabel();
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel bossLabel = new JLabel();
    private final JLabel requirementLabel = new JLabel();
    private final JLabel noteLabel = new JLabel();
    private final JLabel nextBossLabel = new JLabel();
    private final JLabel walletLabel = new JLabel();
    private final JLabel economyLabel = new JLabel();
    private final JLabel evidenceLabel = new JLabel();
    private final JButton completeButton = new JButton("Complete boss");
    private final JButton previousButton = new JButton("Previous");
    private final JButton chooseButton = new JButton("Choose");
    private final JButton resetButton = new JButton("Reset");

    private final JLabel gearBossLabel = new JLabel();
    private final JLabel gearAdviceLabel = new JLabel();
    private final JTextArea meleeArea = gearArea();
    private final JTextArea rangedArea = gearArea();
    private final JTextArea magicArea = gearArea();
    private final JTextArea upgradeArea = gearArea();

    private volatile String upgradeKey = "";
    private volatile UpgradeAdvisor.UpgradePlan upgradePlan;

    public OneBossAtATimePanel(OneBossAtATimePlugin plugin, GearAdvisor gearAdvisor, UpgradeAdvisor upgradeAdvisor)
    {
        this.plugin = plugin;
        this.gearAdvisor = gearAdvisor;
        this.upgradeAdvisor = upgradeAdvisor;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JLabel title = new JLabel("ONE BOSS AT A TIME");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(CENTER_ALIGNMENT);
        add(title);
        add(Box.createRigidArea(new Dimension(0, 6)));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Progress", wrap(createProgressTab()));
        tabs.addTab("Boss Gear", wrap(createGearTab()));
        tabs.setAlignmentX(CENTER_ALIGNMENT);
        add(tabs);
    }

    private JPanel createProgressTab()
    {
        JPanel p = columnPanel();
        progressLabel.setForeground(Color.LIGHT_GRAY);
        progressLabel.setAlignmentX(CENTER_ALIGNMENT);
        p.add(progressLabel);
        p.add(Box.createRigidArea(new Dimension(0, 3)));
        progressBar.setStringPainted(true);
        progressBar.setMaximum(plugin.getStages().size());
        progressBar.setAlignmentX(CENTER_ALIGNMENT);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        p.add(progressBar);
        p.add(Box.createRigidArea(new Dimension(0, 10)));

        p.add(sectionHeader("CURRENT BOSS"));
        bossLabel.setForeground(Color.WHITE);
        bossLabel.setFont(bossLabel.getFont().deriveFont(Font.BOLD, 18f));
        bossLabel.setHorizontalAlignment(SwingConstants.CENTER);
        bossLabel.setAlignmentX(CENTER_ALIGNMENT);
        p.add(bossLabel);
        p.add(Box.createRigidArea(new Dimension(0, 8)));

        p.add(sectionHeader("TO PROGRESS"));
        requirementLabel.setForeground(Color.WHITE);
        requirementLabel.setAlignmentX(CENTER_ALIGNMENT);
        p.add(requirementLabel);
        p.add(Box.createRigidArea(new Dimension(0, 5)));
        noteLabel.setForeground(Color.LIGHT_GRAY);
        noteLabel.setAlignmentX(CENTER_ALIGNMENT);
        p.add(noteLabel);
        p.add(Box.createRigidArea(new Dimension(0, 8)));

        p.add(sectionHeader("AUTO-DETECTION"));
        evidenceLabel.setForeground(new Color(170, 210, 170));
        evidenceLabel.setAlignmentX(CENTER_ALIGNMENT);
        p.add(evidenceLabel);
        p.add(Box.createRigidArea(new Dimension(0, 8)));

        p.add(sectionHeader("CHALLENGE ECONOMY"));
        walletLabel.setForeground(new Color(255, 205, 80));
        walletLabel.setFont(walletLabel.getFont().deriveFont(Font.BOLD, 15f));
        walletLabel.setAlignmentX(CENTER_ALIGNMENT);
        p.add(walletLabel);
        economyLabel.setForeground(Color.LIGHT_GRAY);
        economyLabel.setAlignmentX(CENTER_ALIGNMENT);
        p.add(economyLabel);
        p.add(Box.createRigidArea(new Dimension(0, 8)));

        p.add(sectionHeader("NEXT BOSS"));
        nextBossLabel.setForeground(new Color(255, 190, 60));
        nextBossLabel.setFont(nextBossLabel.getFont().deriveFont(Font.BOLD, 14f));
        nextBossLabel.setHorizontalAlignment(SwingConstants.CENTER);
        nextBossLabel.setAlignmentX(CENTER_ALIGNMENT);
        p.add(nextBossLabel);
        p.add(Box.createRigidArea(new Dimension(0, 10)));

        completeButton.addActionListener(e -> onComplete());
        completeButton.setAlignmentX(CENTER_ALIGNMENT);
        completeButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        p.add(completeButton);
        p.add(Box.createRigidArea(new Dimension(0, 5)));

        JPanel utility = new JPanel(new GridLayout(1, 3, 5, 0));
        utility.setBackground(ColorScheme.DARK_GRAY_COLOR);
        previousButton.addActionListener(e -> plugin.previousStage());
        chooseButton.addActionListener(e -> onChooseStage());
        resetButton.addActionListener(e -> onReset());
        utility.add(previousButton);
        utility.add(chooseButton);
        utility.add(resetButton);
        utility.setMaximumSize(new Dimension(Integer.MAX_VALUE, 29));
        p.add(utility);
        p.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel rule = new JLabel(htmlCentered("Old tradeable equipment is locked. Pre-existing untradeables are always legal. Open your bank once so the BiS tab can discover all of your untradeables.<br><br>Collection Log history: open the in-game Collection Log and view a boss page. Already-obtained requirement items on pages you view are remembered and can satisfy progression, but they do not unlock old tradeable gear or add challenge GP."));
        rule.setForeground(new Color(150, 150, 150));
        rule.setAlignmentX(CENTER_ALIGNMENT);
        p.add(rule);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel createGearTab()
    {
        JPanel p = columnPanel();
        gearBossLabel.setForeground(Color.WHITE);
        gearBossLabel.setFont(gearBossLabel.getFont().deriveFont(Font.BOLD, 17f));
        gearBossLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gearBossLabel.setAlignmentX(CENTER_ALIGNMENT);
        p.add(gearBossLabel);
        p.add(Box.createRigidArea(new Dimension(0, 5)));
        gearAdviceLabel.setForeground(Color.LIGHT_GRAY);
        gearAdviceLabel.setAlignmentX(CENTER_ALIGNMENT);
        p.add(gearAdviceLabel);
        p.add(Box.createRigidArea(new Dimension(0, 8)));

        addGearSection(p, "MELEE", meleeArea);
        addGearSection(p, "RANGED", rangedArea);
        addGearSection(p, "MAGIC", magicArea);
        addGearSection(p, "WALLET UPGRADE ADVISOR", upgradeArea);

        JLabel caveat = new JLabel(htmlCentered("BiS is a boss-aware stat heuristic, not a full DPS simulator. It accounts for the boss style profile, legal gear, offensive stats and important boss-specific item priorities."));
        caveat.setForeground(new Color(145, 145, 145));
        caveat.setAlignmentX(CENTER_ALIGNMENT);
        p.add(caveat);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private void addGearSection(JPanel p, String title, JTextArea area)
    {
        p.add(sectionHeader(title));
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        p.add(area);
        p.add(Box.createRigidArea(new Dimension(0, 8)));
    }

    private static JTextArea gearArea()
    {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(true);
        area.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        area.setForeground(Color.WHITE);
        area.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        area.setFont(area.getFont().deriveFont(11f));
        return area;
    }

    private JPanel columnPanel()
    {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(ColorScheme.DARK_GRAY_COLOR);
        p.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
        return p;
    }

    private JScrollPane wrap(JPanel panel)
    {
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JLabel sectionHeader(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(150, 150, 150));
        label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
        label.setAlignmentX(CENTER_ALIGNMENT);
        return label;
    }

    private void onComplete()
    {
        java.util.List<BossStage> stages = plugin.getStages();
        int index = plugin.getCurrentStageIndex();
        if (index >= stages.size()) return;
        BossStage stage = stages.get(index);
        if (plugin.getConfig().confirmAdvance())
        {
            int result = JOptionPane.showConfirmDialog(this,
                "Manually mark " + stage.getBoss() + " complete?\n\nAutomatic detection is preferred when possible.",
                "Complete boss", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.YES_OPTION) return;
        }
        plugin.advanceStage();
    }

    private void onChooseStage()
    {
        java.util.List<BossStage> stages = plugin.getStages();
        String[] choices = new String[stages.size() + 1];
        for (int i = 0; i < stages.size(); i++) choices[i] = (i + 1) + ". " + stages.get(i).getBoss();
        choices[stages.size()] = "Challenge Complete";
        int current = plugin.getCurrentStageIndex();
        Object selected = JOptionPane.showInputDialog(this, "Choose your current boss stage:", "Choose stage",
            JOptionPane.PLAIN_MESSAGE, null, choices, choices[Math.min(current, choices.length - 1)]);
        if (selected == null) return;
        for (int i = 0; i < choices.length; i++) if (choices[i].equals(selected)) { plugin.setCurrentStageIndex(i); return; }
    }

    private void onReset()
    {
        int result = JOptionPane.showConfirmDialog(this,
            "Reset the entire challenge?\n\nThis wipes boss progress, challenge GP, tradeable gear unlocks and boss-loot sale credit.",
            "Reset challenge", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) plugin.resetProgress();
    }

    public void refresh()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }

        ChallengeState state = plugin.getState();
        java.util.List<BossStage> stages = plugin.getStages();
        int index = plugin.getCurrentStageIndex();
        int total = stages.size();
        int completed = Math.min(index, total);
        progressBar.setMaximum(total);
        progressBar.setValue(completed);
        progressBar.setString(completed + " / " + total);
        progressLabel.setText("Bosses completed: " + completed + " / " + total);
        previousButton.setEnabled(index > 0);
        walletLabel.setText("Wallet: " + OneBossAtATimePlugin.formatGp(state.getWallet()) + " GP");
        long sellableQty = 0;
        for (Long q : state.getSellableBossLoot().values()) sellableQty += q == null ? 0 : q;
        economyLabel.setText(htmlCentered("Earned: " + OneBossAtATimePlugin.formatGp(state.getTotalEarned()) +
            " | Spent: " + OneBossAtATimePlugin.formatGp(state.getTotalSpent()) +
            "<br>Unlocked tradeable gear IDs: " + state.getUnlockedTradeableGear().size() +
            " | Boss-loot units banked for GE sale: " + sellableQty));

        if (index >= total)
        {
            bossLabel.setText(htmlCentered("Challenge Complete!"));
            requirementLabel.setText(htmlCentered("Every checkpoint is complete."));
            noteLabel.setText(htmlCentered("Infernal cape is the final trophy."));
            evidenceLabel.setText(htmlCentered("Finished"));
            nextBossLabel.setText("None — finished!");
            completeButton.setEnabled(false);
            gearBossLabel.setText("Challenge Complete");
            gearAdviceLabel.setText(htmlCentered("No current boss."));
            meleeArea.setText("Complete.");
            rangedArea.setText("Complete.");
            magicArea.setText("Complete.");
            upgradeArea.setText("No further checkpoint upgrades required.");
            return;
        }

        BossStage current = stages.get(index);
        BossStage next = index + 1 < total ? stages.get(index + 1) : null;
        bossLabel.setText(htmlCentered((index + 1) + ". " + current.getBoss()));
        requirementLabel.setText(htmlCentered(current.getRequirement()));
        noteLabel.setText(htmlCentered(current.getNote()));
        nextBossLabel.setText(next == null ? "FINAL STAGE" : htmlCentered(next.getBoss()));
        evidenceLabel.setText(htmlCentered("Stage kills: " + state.getStageKillCount() +
            " | Requirement items seen: " + state.getStageEvidence().size() +
            "<br>Collection Log history synced: " + state.getCollectionLogEvidence().size() +
            " | Log history: " + (plugin.getConfig().collectionLogBackfill() ? "ON" : "OFF") +
            "<br>Auto progression: " + (plugin.getConfig().autoProgress() ? "ON" : "OFF") +
            " | Wilderness: " + (plugin.getConfig().excludeWilderness() ? "EXCLUDED" : "INCLUDED")));
        completeButton.setEnabled(true);

        BossCombatProfile profile = BossCombatProfile.forBoss(current.getBoss());
        gearBossLabel.setText(htmlCentered(current.getBoss()));
        gearAdviceLabel.setText(htmlCentered(profile.getAdvice()));
        Set<Integer> legal = plugin.getLegalKnownItems();

        if (!profile.isExternalGearUsed())
        {
            meleeArea.setText("BEST — External gear is not used. Build your equipment inside the Corrupted Gauntlet.");
            rangedArea.setText("BEST — External gear is not used. Build your equipment inside the Corrupted Gauntlet.");
            magicArea.setText("BEST — External gear is not used. Build your equipment inside the Corrupted Gauntlet.");
        }
        else
        {
            meleeArea.setText(styleText(CombatStyle.MELEE, profile, legal));
            rangedArea.setText(styleText(CombatStyle.RANGED, profile, legal));
            magicArea.setText(styleText(CombatStyle.MAGIC, profile, legal));
        }

        scheduleUpgradeCalculation(index, state.getWallet(), legal, profile);
    }

    private String styleText(CombatStyle style, BossCombatProfile profile, Set<Integer> legal)
    {
        StringBuilder out = new StringBuilder();
        out.append(profile.getRating(style).getLabel()).append('\n');
        Map<Integer, GearAdvisor.GearPick> picks = gearAdvisor.calculate(legal, profile, style);
        if (picks.isEmpty())
        {
            out.append("No challenge-legal equippable items have been observed for this style yet. Open your bank to register pre-existing untradeables.");
            return out.toString();
        }
        for (Map.Entry<Integer, GearAdvisor.GearPick> e : picks.entrySet())
        {
            out.append(GearAdvisor.slotName(e.getKey())).append(": ").append(e.getValue().getName()).append('\n');
        }
        return out.toString().trim();
    }

    private void scheduleUpgradeCalculation(int stage, long wallet, Set<Integer> legal, BossCombatProfile profile)
    {
        String key = stage + ":" + wallet + ":" + legal.hashCode() + ":" + plugin.getMarketCatalogVersion();
        if (key.equals(upgradeKey) && upgradePlan != null)
        {
            upgradeArea.setText(upgradeText(upgradePlan, wallet));
            return;
        }
        upgradeKey = key;
        upgradePlan = null;
        upgradeArea.setText("Scanning challenge-legal and GE equipment for the best boss-specific upgrades…\n" + plugin.getMarketCatalogStatus());
        new SwingWorker<UpgradeAdvisor.UpgradePlan, Void>()
        {
            @Override
            protected UpgradeAdvisor.UpgradePlan doInBackground()
            {
                return upgradeAdvisor.calculate(legal, profile, wallet);
            }

            @Override
            protected void done()
            {
                try
                {
                    if (!key.equals(upgradeKey)) return;
                    upgradePlan = get();
                    upgradeArea.setText(upgradeText(upgradePlan, plugin.getChallengeWallet()));
                }
                catch (Exception ex)
                {
                    upgradeArea.setText("Upgrade scan unavailable. The progression and gear-lock systems continue to work normally.");
                }
            }
        }.execute();
    }

    private String upgradeText(UpgradeAdvisor.UpgradePlan plan, long wallet)
    {
        if (plan == null) return "Calculating…";
        if (plan.getNote() != null && plan.getBuyNow() == null && plan.getNextTarget() == null && plan.getSaveFor() == null)
        {
            return plan.getNote();
        }
        StringBuilder out = new StringBuilder();
        if (plan.getNote() != null) out.append(plan.getNote()).append("\n\n");
        appendCandidate(out, "BUY NOW", plan.getBuyNow(), wallet, true);
        appendCandidate(out, "NEXT TARGET", plan.getNextTarget(), wallet, false);
        appendCandidate(out, "SAVE FOR", plan.getSaveFor(), wallet, false);
        return out.toString().trim();
    }

    private void appendCandidate(StringBuilder out, String label, UpgradeAdvisor.Candidate c, long wallet, boolean showAfter)
    {
        out.append(label).append(": ");
        if (c == null)
        {
            out.append("No meaningful candidate found.\n\n");
            return;
        }
        out.append(c.getName()).append(" — ").append(OneBossAtATimePlugin.formatGp(c.getPrice())).append(" GP")
            .append(" [").append(c.getStyle()).append(", ").append(GearAdvisor.slotName(c.getSlot())).append("]\n");
        if (c.getPrice() > wallet) out.append("Need ").append(OneBossAtATimePlugin.formatGp(c.getPrice() - wallet)).append(" more GP.\n");
        else if (showAfter) out.append("Wallet after purchase: ").append(OneBossAtATimePlugin.formatGp(wallet - c.getPrice())).append(" GP.\n");
        out.append('\n');
    }

    private String htmlCentered(String text)
    {
        return "<html><div style='text-align:center; width:190px'>" + escapeHtmlExceptBreaks(text) + "</div></html>";
    }

    private String escapeHtmlExceptBreaks(String text)
    {
        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
        return safe.replace("&lt;br&gt;", "<br>");
    }
}
