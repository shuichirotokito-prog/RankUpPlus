package com.kazumiii.rankupplus;

import com.kazumiii.rankupplus.commands.RankAdminCommand;
import com.kazumiii.rankupplus.commands.RankUpCommand;
import com.kazumiii.rankupplus.commands.RanksCommand;
import com.kazumiii.rankupplus.commands.RankUpGuiCommand;
import com.kazumiii.rankupplus.commands.RankTopCommand;
import com.kazumiii.rankupplus.gui.GuiListener;
import com.kazumiii.rankupplus.listeners.LegacyChatListener;
import com.kazumiii.rankupplus.listeners.PaperChatListener;
import com.kazumiii.rankupplus.listeners.PlayerListener;
import com.kazumiii.rankupplus.managers.*;
import com.kazumiii.rankupplus.placeholders.RankUpPlusExpansion;
import com.kazumiii.rankupplus.tasks.AutoRankupTask;
import com.kazumiii.rankupplus.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class RankUpPlus extends JavaPlugin {

    private static RankUpPlus instance;

    private ConfigManager configManager;
    private RankManager rankManager;
    private PlayerDataManager playerDataManager;
    private MessageManager messageManager;
    private GuiManager guiManager;
    private PrestigeManager prestigeManager;
    private EconomyManager economyManager;
    private RequirementManager requirementManager;
    private RankUpManager rankUpManager;
    private PlayerListener playerListener;
    private LuckPermsHook luckPermsHook;
    private BoosterManager boosterManager;
    private RankScoreboardManager scoreboardManager;
    private LeaderboardManager leaderboardManager;
    private ChatManager chatManager;
    private PapiBridge papiBridge;
    private MetricsManager metricsManager;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        instance = this;

        if (isFolia()) {
            getLogger().severe("========================================");
            getLogger().severe("RankUpPlus does not support Folia yet.");
            getLogger().severe("This plugin relies on Bukkit's global task scheduler");
            getLogger().severe("(for playtime tracking, scoreboards, boosters, and");
            getLogger().severe("leaderboards), which Folia does not allow plugins to");
            getLogger().severe("use safely across its regionized threading model.");
            getLogger().severe("Please run this plugin on Paper, Spigot, or Purpur instead.");
            getLogger().severe("========================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        printBanner();

        try {
            // Initialize managers in order
            this.configManager = new ConfigManager(this);
            this.messageManager = new MessageManager(this);
            this.economyManager = new EconomyManager(this);
            this.rankManager = new RankManager(this);
            this.luckPermsHook = new LuckPermsHook(this);
            this.papiBridge = new PapiBridge(this);
            this.boosterManager = new BoosterManager(this);
            this.playerDataManager = new PlayerDataManager(this);
            this.prestigeManager = new PrestigeManager(this);
            this.requirementManager = new RequirementManager(this);
            this.guiManager = new GuiManager(this);
            this.rankUpManager = new RankUpManager(this);
            this.scoreboardManager = new RankScoreboardManager(this);
            this.leaderboardManager = new LeaderboardManager(this);
            this.chatManager = new ChatManager(this);

            if (!economyManager.isSetup()) {
                getLogger().warning("Economy (Vault) not found! Switching to XP mode.");
            }

            // Register commands
            registerCommands();

            // Register listeners
            registerListeners();

            // PlaceholderAPI — provide our own placeholders
            if (configManager.isPlaceholderApiIntegration()
                    && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new RankUpPlusExpansion(this).register();
                getLogger().info("PlaceholderAPI hooked — providing %rankupplus_*% placeholders.");
                getLogger().info("Other plugins' placeholders also work in scoreboard/chat lines via PapiBridge.");
            }

            // Auto-rankup task
            if (configManager.isAutoRankup()) {
                new AutoRankupTask(this).runTaskTimer(this, 100L, 100L);
            }

            // Periodically flush online players' session playtime
            Bukkit.getScheduler().runTaskTimer(this, () -> {
                if (playerListener != null) playerListener.flushAllSessionPlaytime();
            }, 1200L, 1200L);

            // Periodically refresh the scoreboard sidebar
            Bukkit.getScheduler().runTaskTimer(this,
                () -> scoreboardManager.refreshAll(),
                40L, configManager.getScoreboardUpdateInterval() * 20L);

            // Periodically expire boosters
            Bukkit.getScheduler().runTaskTimer(this,
                () -> boosterManager.tickExpirations(),
                200L, 200L);

            // Initial + periodic leaderboard refresh
            leaderboardManager.refreshAsync();
            Bukkit.getScheduler().runTaskTimer(this,
                leaderboardManager::refreshAsync,
                1200L, configManager.getLeaderboardRefreshMinutes() * 1200L);

            // bStats metrics (respects the server-wide plugins/bStats/config.yml opt-out)
            this.metricsManager = new MetricsManager(this);

            // Update checker — async, never blocks startup, silently skips
            // any platform that isn't configured with a real id/slug yet
            this.updateChecker = new UpdateChecker(this);
            updateChecker.checkAsync();

            getLogger().info("RankUpPlus v" + getDescription().getVersion() + " enabled!");

        } catch (Throwable t) {
            getLogger().severe("========================================");
            getLogger().severe("RankUpPlus FAILED to enable! Error:");
            getLogger().severe(t.getClass().getSimpleName() + ": " + t.getMessage());
            for (StackTraceElement el : t.getStackTrace()) {
                getLogger().severe("  at " + el);
            }
            getLogger().severe("========================================");
            getLogger().severe("The plugin has been DISABLED. Fix the error above and restart.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (playerListener != null) {
            playerListener.flushAllSessionPlaytime();
        }
        // Defensive cleanup in case of a plugin disable without a full server stop
        // (e.g. /reload) — return players to the default scoreboard.
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        if (playerDataManager != null) {
            playerDataManager.saveAll();
            playerDataManager.close();
        }
        getLogger().info("RankUpPlus disabled. All data saved.");
    }

    private void registerCommands() {
        getCommand("rankup").setExecutor(new RankUpCommand(this));
        getCommand("rankup").setTabCompleter(new RankUpCommand(this));
        getCommand("rankupgui").setExecutor(new RankUpGuiCommand(this));
        getCommand("ranks").setExecutor(new RanksCommand(this));
        getCommand("rankadmin").setExecutor(new RankAdminCommand(this));
        getCommand("rankadmin").setTabCompleter(new RankAdminCommand(this));
        getCommand("ranktop").setExecutor(new RankTopCommand(this));
        getCommand("ranktop").setTabCompleter(new RankTopCommand(this));
    }

    private void registerListeners() {
        playerListener = new PlayerListener(this);
        Bukkit.getPluginManager().registerEvents(playerListener, this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        registerChatListener();
    }

    /**
     * Registers whichever chat listener actually works on this server.
     * Detection uses Class.forName with a STRING literal class name — this is
     * deliberate. Directly referencing io.papermc.paper.event.player.AsyncChatEvent
     * as a type anywhere (a field, a method parameter, an import used in a
     * signature) forces the JVM to resolve that class the moment the
     * containing class loads, which throws NoClassDefFoundError on plain
     * Spigot the instant that class is touched — regardless of whether the
     * code path that uses it actually runs. Class.forName(String) has no such
     * eager-resolution problem, so it's the safe way to ask "does this class
     * exist here?" without risking a crash on servers where it doesn't.
     */
    private void registerChatListener() {
        boolean paperChatApiAvailable;
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            paperChatApiAvailable = true;
        } catch (ClassNotFoundException e) {
            paperChatApiAvailable = false;
        }

        if (paperChatApiAvailable) {
            Bukkit.getPluginManager().registerEvents(new PaperChatListener(this), this);
            getLogger().info("Using Paper's modern chat API (AsyncChatEvent) for chat formatting.");
        } else {
            Bukkit.getPluginManager().registerEvents(new LegacyChatListener(this), this);
            getLogger().info("Paper's chat API not found (likely running on Spigot/CraftBukkit) — "
                + "using the legacy AsyncPlayerChatEvent for chat formatting instead.");
        }
    }

    /**
     * Detects Folia (PaperMC's regionized-multithreading fork) using the
     * detection method documented by PaperMC itself: checking for a
     * Folia-exclusive class that doesn't exist on Paper, Spigot, or Purpur.
     *
     * This plugin doesn't support Folia yet — its scheduling (playtime
     * flush, scoreboard refresh, booster expiry, leaderboard refresh,
     * auto-rankup) all uses Bukkit's global task scheduler, which Folia
     * deliberately refuses to run for plugins (regionized multithreading
     * makes "run this on the main thread" ambiguous — there is no single
     * main thread — so Folia throws rather than risk silently incorrect
     * behavior).
     *
     * In practice, Folia's own plugin loader already won't load ANY plugin
     * that doesn't explicitly declare `folia-supported: true` in plugin.yml
     * — which this plugin deliberately does not — so this check mostly
     * exists to give a clear, RankUpPlus-specific explanation rather than
     * rely solely on Folia's generic loader-level rejection message.
     */
    private boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void printBanner() {
        String[] banner = {
            "",
            ColorUtils.color("&b  ____             _    _   _       _____  _             "),
            ColorUtils.color("&b |  _ \\ __ _ _ __ | | _| | | |_ __ |  __ \\| |_   _ ___  "),
            ColorUtils.color("&b | |_) / _` | '_ \\| |/ / | | | '_ \\| |__) | | | | / __| "),
            ColorUtils.color("&b |  _ < (_| | | | |   <| |_| | |_) |  ___/| | |_| \\__ \\ "),
            ColorUtils.color("&b |_| \\_\\__,_|_| |_|_|\\_\\\\___/| .__/|_|   |_|\\__,_|___/ "),
            ColorUtils.color("&b                              |_|                         "),
            ColorUtils.color("&3   Advanced Rankup Plugin &8| &7by &bKazumiii"),
            ""
        };
        for (String line : banner) {
            Bukkit.getConsoleSender().sendMessage(line);
        }
    }

    public void reload() {
        if (playerListener != null) {
            playerListener.flushAllSessionPlaytime();
        }
        configManager.reload();
        rankManager.reload();
        messageManager.reload();
        playerDataManager.reload();
        guiManager.reload();
        // Reset soft-dependency hooks so they re-detect availability after reload
        papiBridge = new PapiBridge(this);
        luckPermsHook = new LuckPermsHook(this);
    }

    // ---- Getters ----
    public static RankUpPlus getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public RankManager getRankManager() { return rankManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public PrestigeManager getPrestigeManager() { return prestigeManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public RequirementManager getRequirementManager() { return requirementManager; }
    public RankUpManager getRankUpManager() { return rankUpManager; }
    public LuckPermsHook getLuckPermsHook() { return luckPermsHook; }
    public BoosterManager getBoosterManager() { return boosterManager; }
    public RankScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public LeaderboardManager getLeaderboardManager() { return leaderboardManager; }
    public ChatManager getChatManager() { return chatManager; }
    public PapiBridge getPapiBridge() { return papiBridge; }
    public MetricsManager getMetricsManager() { return metricsManager; }
    public UpdateChecker getUpdateChecker() { return updateChecker; }
}
