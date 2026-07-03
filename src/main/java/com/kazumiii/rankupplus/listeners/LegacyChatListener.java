package com.kazumiii.rankupplus.listeners;

import com.kazumiii.rankupplus.RankUpPlus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Spigot-compatible chat formatting fallback, used when Paper's modern
 * AsyncChatEvent isn't available (plain Spigot/CraftBukkit). Uses the
 * legacy AsyncPlayerChatEvent — deprecated on Paper in favor of
 * AsyncChatEvent, but still the only chat event plain Spigot has, and it
 * remains fully functional there.
 *
 * Runs at HIGH priority (after most other plugins' LOWEST/LOW/NORMAL
 * handlers) so that if another plugin narrows event.getRecipients() — e.g. a
 * chat-channel or ignore-player plugin — those changes are respected: we
 * read the recipient set as it stands at HIGH, then take over delivery
 * ourselves with our own rendered format, rather than using
 * event.setFormat()'s String.format(%1$s/%2$s) substitution, since chat.yml's
 * formats are freeform rather than a fixed prefix+name+suffix+message shape.
 */
public class LegacyChatListener implements Listener {

    private final RankUpPlus plugin;

    public LegacyChatListener(RankUpPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!plugin.getChatManager().isEnabled()) return;

        String rendered = plugin.getChatManager().renderLegacy(event.getPlayer(), event.getMessage());

        // Take over delivery ourselves instead of using setFormat(), then
        // cancel so CraftBukkit's own default post-event delivery doesn't
        // also send an unformatted copy.
        for (Player recipient : event.getRecipients()) {
            recipient.sendMessage(rendered);
        }
        Bukkit.getConsoleSender().sendMessage(rendered);
        event.setCancelled(true);
    }
}
