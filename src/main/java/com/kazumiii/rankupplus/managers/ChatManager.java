package com.kazumiii.rankupplus.managers;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.models.PlayerData;
import com.kazumiii.rankupplus.models.Rank;
import com.kazumiii.rankupplus.utils.ColorUtils;
import org.bukkit.entity.Player;

/**
 * Resolves and renders per-rank chat formats. A player's format is looked up
 * by their current rank id in chat.yml's per-rank section, falling back to
 * the global default-format if no override exists for that rank.
 *
 * Deliberately produces a plain legacy (§-coded) String, not an Adventure
 * Component — this class must be safe to load on both Paper AND plain
 * Spigot/CraftBukkit servers, and Adventure's text API is Paper-exclusive.
 * The actual event wiring (PaperChatListener using AsyncChatEvent, or
 * LegacyChatListener using AsyncPlayerChatEvent as a Spigot-safe fallback)
 * lives in separate classes chosen at runtime — see RankUpPlus#registerListeners.
 *
 * SECURITY NOTE: the player's actual message text is deliberately kept out of
 * the string that gets color-translated and PlaceholderAPI-processed. If a
 * player's raw chat text were substituted into the format string before those
 * two steps ran, a message like "&4&lFAKE ADMIN" or a string containing a "%"
 * placeholder pattern could let players inject color codes or trigger
 * PlaceholderAPI placeholder evaluation through their own chat — including,
 * in the worst case, relational placeholders that read data about OTHER
 * players. Instead: the surrounding format (everything except the message)
 * has fields/PAPI/color applied FIRST, using a unique token in place of
 * {message}; the player's raw message is substituted in AFTER color
 * translation has already run, so any '&' in it is never interpreted. Any
 * literal '§' the player's client might have sent directly (bypassing '&'
 * translation entirely) is also stripped, since the Minecraft client renders
 * raw § as a color code regardless of server-side &-translation.
 */
public class ChatManager {

    private final RankUpPlus plugin;
    private static final String MESSAGE_TOKEN = "\u0000RUP_MSG\u0000";

    public ChatManager(RankUpPlus plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfigManager().isChatFormatEnabled();
    }

    /**
     * Builds the final rendered legacy (§-coded) chat line for a message from
     * the given player, applying their rank's chat format (or the default),
     * all RankUpPlus placeholders, and any installed PlaceholderAPI
     * placeholders — to the surrounding format only, never to the player's
     * own message text.
     */
    public String renderLegacy(Player player, String rawMessage) {
        String format = resolveFormat(player);

        // Substitute everything except {message}, leaving a unique token in
        // its place so color translation and PAPI run only on the surrounding
        // format, never on the player's own text.
        String withFields = applyFields(format, player);
        String withPapi = plugin.getPapiBridge().apply(player, withFields);
        String colored = ColorUtils.color(withPapi);

        String safeMessage = ColorUtils.stripLiteralSectionSigns(rawMessage);

        int tokenIndex = colored.indexOf(MESSAGE_TOKEN);
        if (tokenIndex < 0) {
            // No {message} placeholder in the configured format at all (a
            // server owner misconfiguration) — still send the message so it
            // isn't silently swallowed, just appended at the end.
            return colored + safeMessage;
        }

        String before = colored.substring(0, tokenIndex);
        String after = colored.substring(tokenIndex + MESSAGE_TOKEN.length());
        return before + safeMessage + after;
    }

    private String resolveFormat(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        String rankId = data != null ? data.getCurrentRankId() : "DEFAULT";
        String override = plugin.getConfigManager().getPerRankChatFormat(rankId);
        return override != null ? override : plugin.getConfigManager().getDefaultChatFormat();
    }

    private String applyFields(String format, Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        Rank rank = data != null ? plugin.getRankManager().getRank(data.getCurrentRankId()) : null;
        int prestige = data != null ? data.getPrestige() : 0;

        return format
            .replace("{player}", player.getName())
            .replace("{rank_color}", rank != null ? rank.getColor() : "7")
            .replace("{rank_display}", rank != null ? rank.getDisplay() : (data != null ? data.getCurrentRankId() : "DEFAULT"))
            .replace("{prestige}", String.valueOf(prestige))
            .replace("{prestige_display}", plugin.getPrestigeManager().getPrestigeDisplay(prestige))
            .replace("{message}", MESSAGE_TOKEN);
    }
}
