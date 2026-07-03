package com.kazumiii.rankupplus.managers;

import com.kazumiii.rankupplus.RankUpPlus;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;

/**
 * Wraps bStats metrics setup. bStats is bundled directly into this plugin's
 * own jar (shaded + relocated in build.gradle), so — unlike LuckPerms or
 * PlaceholderAPI — there's no "might not be installed" concern here; it's
 * always present. Server owners can still opt out globally via bStats' own
 * shared plugins/bStats/config.yml, which Metrics respects automatically.
 *
 * IMPORTANT: pluginId below is a placeholder. Register this plugin at
 * https://bstats.org (sign in, "My Plugins" -> add plugin) once it's been
 * published, then replace PLUGIN_ID with the real id bStats assigns you.
 * Metrics will still run with the placeholder, it just won't correspond to
 * any dashboard you can see — a fresh id will start its own clean history,
 * so it's worth setting the real one before your first real release rather
 * than after.
 */
public class MetricsManager {

    private static final int PLUGIN_ID = 00000; // TODO: replace with your real bStats plugin id

    public MetricsManager(RankUpPlus plugin) {
        Metrics metrics = new Metrics(plugin, PLUGIN_ID);

        metrics.addCustomChart(new SimplePie("storage_type", () ->
            plugin.getConfigManager().getStorageType().equalsIgnoreCase("MYSQL") ? "MySQL" : "YAML"));

        metrics.addCustomChart(new SimplePie("economy_backend", () ->
            plugin.getEconomyManager().isSetup() ? "Vault" : "XP Levels"));

        metrics.addCustomChart(new SimplePie("luckperms_integration", () ->
            plugin.getLuckPermsHook().isAvailable() ? "Enabled" : "Disabled"));

        metrics.addCustomChart(new SimplePie("chat_format_enabled", () ->
            plugin.getChatManager().isEnabled() ? "Enabled" : "Disabled"));

        metrics.addCustomChart(new SimplePie("scoreboard_enabled", () ->
            plugin.getConfigManager().isScoreboardEnabled() ? "Enabled" : "Disabled"));
    }
}
