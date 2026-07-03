package com.kazumiii.rankupplus.listeners;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.models.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final RankUpPlus plugin;
    private final Map<UUID, Long> joinTimes = new HashMap<>();

    public PlayerListener(RankUpPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        joinTimes.put(uuid, System.currentTimeMillis());

        plugin.getPlayerDataManager().loadAsync(uuid, name).thenAccept(data ->
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return; // They disconnected before the load finished.

                data.setName(name);
                plugin.getPlayerDataManager().cachePut(uuid, data);

                plugin.getScoreboardManager().setupPlayer(player);

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) ensureRankGroup(player, data);
                }, 20L);
            })
        ).exceptionally(ex -> {
            plugin.getLogger().severe("Failed to load data for " + name + ": " + ex.getMessage());
            return null;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        flushSessionPlaytime(uuid);
        joinTimes.remove(uuid);
        plugin.getScoreboardManager().teardownPlayer(uuid, player.getName());
        plugin.getPlayerDataManager().unload(uuid);
    }

    /**
     * Credits whatever playtime has accumulated since the last flush (join or
     * previous flush) to the player's PlayerData, then resets the session
     * baseline. Safe to call repeatedly (e.g. from a periodic task) so that
     * playtime-based requirements update live instead of only on logout.
     */
    public void flushSessionPlaytime(UUID uuid) {
        Long start = joinTimes.get(uuid);
        if (start == null) return;
        long now = System.currentTimeMillis();
        long sessionMinutes = (now - start) / 60000;
        if (sessionMinutes <= 0) return;
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(uuid);
        if (data != null) {
            data.addPlaytime(sessionMinutes);
        }
        // Advance the baseline by exactly the whole minutes we credited,
        // keeping any leftover seconds so they aren't lost between flushes.
        joinTimes.put(uuid, start + sessionMinutes * 60000);
    }

    public void flushAllSessionPlaytime() {
        for (UUID uuid : new java.util.ArrayList<>(joinTimes.keySet())) {
            flushSessionPlaytime(uuid);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data != null) data.incrementDeaths();

        // Track killer
        Player killer = player.getKiller();
        if (killer != null && !killer.equals(player)) {
            PlayerData killerData = plugin.getPlayerDataManager().getPlayerData(killer.getUniqueId());
            if (killerData != null) killerData.incrementKills();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(event.getPlayer().getUniqueId());
        if (data != null) data.incrementBlocksBroken();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(event.getPlayer().getUniqueId());
        if (data != null) data.incrementBlocksPlaced();
    }

    private void ensureRankGroup(Player player, PlayerData data) {
        String rankId = data.getCurrentRankId();
        com.kazumiii.rankupplus.models.Rank rank = plugin.getRankManager().getRank(rankId);
        if (rank != null) {
            plugin.getLuckPermsHook().addGroupIfMissing(player, rank.getPermission());
        }
    }
}
