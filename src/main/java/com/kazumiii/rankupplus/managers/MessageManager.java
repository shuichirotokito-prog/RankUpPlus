package com.kazumiii.rankupplus.managers;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.models.Rank;
import com.kazumiii.rankupplus.utils.ColorUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;

public class MessageManager {

    private final RankUpPlus plugin;
    private FileConfiguration config;
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.##");
    private static final DecimalFormat COMPACT_FORMAT = new DecimalFormat("#,##0.#");

    public MessageManager(RankUpPlus plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        this.config = plugin.getConfig();
    }

    public String getPrefix() {
        return ColorUtils.color(plugin.getConfigManager().getPrefix());
    }

    public String getMessage(String key) {
        String msg = config.getString("messages." + key, "&cMissing message: " + key);
        return ColorUtils.color(getPrefix() + msg);
    }

    /**
     * Returns the raw message template for a key, with no prefix and no color
     * translation applied. Use this when placeholders (like {rank_color}) need
     * to be substituted before final colorization — passing an already-colored,
     * already-prefixed string into sendRaw() would double up the prefix.
     */
    public String getRawMessage(String key) {
        return config.getString("messages." + key, "&cMissing message: " + key);
    }

    public String getMessage(String key, String... replacements) {
        String msg = getMessage(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(getMessage(key));
    }

    public void send(CommandSender sender, String key, String... replacements) {
        sender.sendMessage(getMessage(key, replacements));
    }

    public void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(ColorUtils.color(getPrefix() + message));
    }

    public String applyRankPlaceholders(String text, Rank rank) {
        return text
            .replace("{rank_display}", rank.getDisplay())
            .replace("{rank_color}", rank.getColor())
            .replace("{rank_id}", rank.getId());
    }

    public String applyRankPlaceholders(String text, Rank rank, Player player) {
        return applyRankPlaceholders(text, rank)
            .replace("{player}", player.getName())
            .replace("{balance}", formatCompactNumber(plugin.getEconomyManager().getBalance(player)));
    }

    /**
     * Formats a raw number compactly: 999 -> "999", 1,500 -> "1.5K",
     * 10,000,000 -> "10M", 12,345,678 -> "12.3M", etc. No currency symbol or
     * suffix — just the abbreviated number. Shared by formatMoney() and
     * EconomyManager#formatCurrency() so both always agree on how large
     * numbers get abbreviated, rather than maintaining two separate
     * implementations that can silently drift apart.
     */
    public static String formatCompactNumber(double amount) {
        if (amount >= 1_000_000_000) return COMPACT_FORMAT.format(amount / 1_000_000_000) + "B";
        if (amount >= 1_000_000) return COMPACT_FORMAT.format(amount / 1_000_000) + "M";
        if (amount >= 1_000) return COMPACT_FORMAT.format(amount / 1_000) + "K";
        return MONEY_FORMAT.format(amount);
    }

    public static String formatMoney(double amount) {
        return formatCompactNumber(amount);
    }

    public static String formatTime(long seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    }
}
