package com.kazumiii.rankupplus.managers;

import com.kazumiii.rankupplus.RankUpPlus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Thin outer wrapper around PlaceholderAPI's setPlaceholders() call. Same
 * isolation pattern as LuckPermsHook/LuckPermsImpl, for the same reason:
 * PlaceholderAPI is a soft/optional (compileOnly) dependency, so this class
 * itself must reference no me.clip.placeholderapi.* types or its mere
 * presence on the classpath would risk a NoClassDefFoundError on servers
 * that don't have PlaceholderAPI installed.
 *
 * Apply this to any user-configurable text (scoreboard lines, chat formats,
 * requirement display strings) so server owners can use ANY installed
 * plugin's placeholders, not just RankUpPlus's own.
 */
public class PapiBridge {

    private final RankUpPlus plugin;
    private PapiImpl impl;
    private boolean checked = false;
    private boolean available = false;

    public PapiBridge(RankUpPlus plugin) {
        this.plugin = plugin;
    }

    public boolean isAvailable() {
        if (!plugin.getConfigManager().isPlaceholderApiIntegration()) return false;
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return false;

        if (!checked) {
            checked = true;
            try {
                impl = new PapiImpl();
                available = true;
            } catch (Throwable t) {
                plugin.getLogger().warning("PlaceholderAPI was detected but could not be hooked: " + t.getMessage());
                impl = null;
                available = false;
            }
        }
        return available;
    }

    /**
     * Applies every registered PlaceholderAPI placeholder in the given text
     * for the given player. If PlaceholderAPI isn't available, returns the
     * text unchanged. Safe to call on every line unconditionally.
     */
    public String apply(Player player, String text) {
        if (text == null || !isAvailable()) return text;
        return impl.apply(player, text);
    }
}
