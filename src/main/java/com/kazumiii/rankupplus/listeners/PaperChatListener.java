package com.kazumiii.rankupplus.listeners;

import com.kazumiii.rankupplus.RankUpPlus;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Paper-exclusive chat formatting via the modern AsyncChatEvent/ChatRenderer
 * API. Only ever registered by RankUpPlus after confirming (via reflection,
 * not a direct class reference) that this event class actually exists on the
 * running server — see RankUpPlus#registerChatListener. Never reference this
 * class directly from anywhere that runs unconditionally at startup.
 */
public class PaperChatListener implements Listener {

    private final RankUpPlus plugin;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    public PaperChatListener(RankUpPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getChatManager().isEnabled()) return;
        String rawMessage = PLAIN.serialize(event.message());

        // viewerUnaware: our format doesn't depend on who's viewing it (same
        // message for every recipient), which Paper's docs note is both
        // correct here and a performance win — the message is only rendered
        // once instead of once per online player.
        event.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, message) ->
            LEGACY.deserialize(plugin.getChatManager().renderLegacy(event.getPlayer(), rawMessage))
        ));
    }
}
