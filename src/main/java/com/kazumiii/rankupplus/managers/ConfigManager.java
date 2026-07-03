package com.kazumiii.rankupplus.managers;

import com.kazumiii.rankupplus.RankUpPlus;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;

/**
 * Owns config.yml plus the four split-out files (ranks.yml, scoreboard.yml,
 * tablist.yml, chat.yml). Each file is wrapped in a YamlFile so it gets the
 * same "save the bundled default if missing, merge in new defaults on load"
 * behavior config.yml already got for free from JavaPlugin#saveDefaultConfig().
 *
 * Other managers (RankManager, RankScoreboardManager, ChatManager, etc.) read
 * from the specific getRanksConfig()/getScoreboardConfig()/etc. accessor for
 * their file, rather than everything sharing plugin.getConfig().
 */
public class ConfigManager {

    private final RankUpPlus plugin;
    private FileConfiguration config;

    private YamlFile ranksFile;
    private YamlFile scoreboardFile;
    private YamlFile tablistFile;
    private YamlFile chatFile;

    public ConfigManager(RankUpPlus plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();

        this.ranksFile = new YamlFile(plugin, "ranks.yml");
        this.scoreboardFile = new YamlFile(plugin, "scoreboard.yml");
        this.tablistFile = new YamlFile(plugin, "tablist.yml");
        this.chatFile = new YamlFile(plugin, "chat.yml");
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        ranksFile.reload();
        scoreboardFile.reload();
        tablistFile.reload();
        chatFile.reload();
    }

    public FileConfiguration getConfig() { return config; }
    public YamlConfiguration getRanksConfig() { return ranksFile.getConfig(); }
    public YamlConfiguration getScoreboardConfig() { return scoreboardFile.getConfig(); }
    public YamlConfiguration getTablistConfig() { return tablistFile.getConfig(); }
    public YamlConfiguration getChatConfig() { return chatFile.getConfig(); }

    /** Used by the admin rank editor GUI to persist an edited field back to ranks.yml. */
    public void saveRanksFile() { ranksFile.save(); }

    // =================== settings (config.yml) ===================

    public String getPrefix() {
        return config.getString("settings.prefix", "&8[&bRankUp&3+&8] &r");
    }

    public String getCurrencyType() {
        return config.getString("settings.currency-type", "VAULT");
    }

    public boolean isBroadcastRankup() {
        return config.getBoolean("settings.broadcast-rankup", true);
    }

    public String getBroadcastFormat() {
        return config.getString("settings.broadcast-format", "");
    }

    public String getRankupSound() {
        return config.getString("settings.rankup-sound", "ENTITY_PLAYER_LEVELUP");
    }

    public float getRankupSoundVolume() {
        return (float) config.getDouble("settings.rankup-sound-volume", 1.0);
    }

    public float getRankupSoundPitch() {
        return (float) config.getDouble("settings.rankup-sound-pitch", 1.2);
    }

    public boolean isFireworkOnRankup() {
        return config.getBoolean("settings.firework-on-rankup", true);
    }

    public boolean isShowTitle() {
        return config.getBoolean("settings.show-title", true);
    }

    public String getRankupTitle() {
        return config.getString("settings.title", "&b✦ RANK UP! ✦");
    }

    public String getRankupSubtitle() {
        return config.getString("settings.subtitle", "&7You are now &{rank_color}{rank_display}");
    }

    public int getTitleFadeIn() { return config.getInt("settings.title-fade-in", 10); }
    public int getTitleStay() { return config.getInt("settings.title-stay", 60); }
    public int getTitleFadeOut() { return config.getInt("settings.title-fade-out", 20); }

    public boolean isConfirmRankup() {
        return config.getBoolean("settings.confirm-rankup", true);
    }

    public boolean isAutoRankup() {
        return config.getBoolean("settings.auto-rankup", false);
    }

    public boolean isLuckPermsIntegration() {
        return config.getBoolean("settings.luckperms-integration", true);
    }

    public boolean isPlaceholderApiIntegration() {
        return config.getBoolean("settings.placeholderapi-integration", true);
    }

    public long getRankupCooldown() {
        return config.getLong("settings.rankup-cooldown", 0);
    }

    // ---------------- Update checker ----------------

    public boolean isUpdateCheckerEnabled() {
        return config.getBoolean("settings.update-checker.enabled", true);
    }

    // =================== prestige (ranks.yml) ===================

    public boolean isPrestigeEnabled() {
        return getRanksConfig().getBoolean("prestige.enabled", true);
    }

    public int getMaxPrestige() {
        return getRanksConfig().getInt("prestige.max-prestige", 10);
    }

    public boolean isPrestigeResetRank() {
        return getRanksConfig().getBoolean("prestige.reset-rank", true);
    }

    public boolean isPrestigeKeepBalance() {
        return getRanksConfig().getBoolean("prestige.keep-balance", false);
    }

    public boolean isPrestigeResetStats() {
        return getRanksConfig().getBoolean("prestige.reset-stats", false);
    }

    public double getPrestigeCostMultiplier() {
        return getRanksConfig().getDouble("prestige.cost-multiplier", 1.5);
    }

    public String getPrestigeDisplayFormat() {
        return getRanksConfig().getString("prestige.display-format", "&5[P{prestige}]");
    }

    // =================== gui (config.yml) ===================

    public String getFillerMaterial() {
        return config.getString("gui.filler-material", "BLACK_STAINED_GLASS_PANE");
    }

    public int getProgressBarLength() {
        return config.getInt("gui.progress-bar.length", 20);
    }

    public String getProgressBarFilled() {
        return config.getString("gui.progress-bar.filled-char", "■");
    }

    public String getProgressBarEmpty() {
        return config.getString("gui.progress-bar.empty-char", "□");
    }

    public String getProgressBarFilledColor() {
        return config.getString("gui.progress-bar.filled-color", "&a");
    }

    public String getProgressBarEmptyColor() {
        return config.getString("gui.progress-bar.empty-color", "&8");
    }

    // ---------------- Storage ----------------

    public String getStorageType() {
        return config.getString("settings.storage-type", "YAML");
    }

    public String getMySQLHost() { return config.getString("mysql.host", "localhost"); }
    public int getMySQLPort() { return config.getInt("mysql.port", 3306); }
    public String getMySQLDatabase() { return config.getString("mysql.database", "rankupplus"); }
    public String getMySQLUsername() { return config.getString("mysql.username", "root"); }
    public String getMySQLPassword() { return config.getString("mysql.password", ""); }
    public String getMySQLTablePrefix() { return config.getString("mysql.table-prefix", "rup_"); }
    public boolean isMySQLUseSSL() { return config.getBoolean("mysql.use-ssl", false); }

    // ---------------- Scoreboard (scoreboard.yml) ----------------

    public boolean isScoreboardEnabled() {
        return getScoreboardConfig().getBoolean("scoreboard.enabled", true);
    }

    public boolean isScoreboardDefaultOn() {
        return getScoreboardConfig().getBoolean("scoreboard.default-on", true);
    }

    public int getScoreboardUpdateInterval() {
        return Math.max(1, getScoreboardConfig().getInt("scoreboard.update-interval-seconds", 2));
    }

    public String getScoreboardTitle() {
        return getScoreboardConfig().getString("scoreboard.title", "&b&lRankUp&3&l+");
    }

    public List<String> getScoreboardLines() {
        return getScoreboardConfig().getStringList("scoreboard.lines");
    }

    // ---------------- Tablist (tablist.yml) ----------------

    public boolean isTablistEnabled() {
        return getTablistConfig().getBoolean("tablist.enabled", true);
    }

    public boolean isTablistSortByRank() {
        return getTablistConfig().getBoolean("tablist.sort-by-rank", true);
    }

    public boolean isTablistShowRankPrefix() {
        return getTablistConfig().getBoolean("tablist.show-rank-prefix", true);
    }

    public String getTablistPrefixFormat() {
        return getTablistConfig().getString("tablist.prefix-format", "&{rank_color}[{rank_display}] &r");
    }

    // ---------------- Chat (chat.yml) ----------------

    public boolean isChatFormatEnabled() {
        return getChatConfig().getBoolean("chat.enabled", true);
    }

    public String getDefaultChatFormat() {
        return getChatConfig().getString("chat.default-format",
            "&7[&{rank_color}{rank_display}&7]{prestige_display} &f{player}&8: &7{message}");
    }

    /** Returns the per-rank chat format override, or null if none is set for this rank. */
    public String getPerRankChatFormat(String rankId) {
        return getChatConfig().getString("chat.per-rank." + rankId, null);
    }

    // ---------------- Leaderboards (config.yml) ----------------

    public int getLeaderboardSize() {
        return Math.max(1, config.getInt("leaderboards.size", 10));
    }

    public int getLeaderboardRefreshMinutes() {
        return Math.max(1, config.getInt("leaderboards.refresh-minutes", 5));
    }
}
