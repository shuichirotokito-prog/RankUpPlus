package com.kazumiii.rankupplus.managers;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.models.PlayerData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class LeaderboardManager {

    public enum Stat {
        RANK, PRESTIGE, PLAYTIME, KILLS, DEATHS, BLOCKS_BROKEN, BLOCKS_PLACED
    }

    private final RankUpPlus plugin;
    private final AtomicReference<List<PlayerData>> cache = new AtomicReference<>(new ArrayList<>());
    private volatile long lastRefresh = 0;

    public LeaderboardManager(RankUpPlus plugin) {
        this.plugin = plugin;
    }

    /** Kicks off an async reload of the full player-data set used for leaderboards. */
    public void refreshAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<PlayerData> all = plugin.getPlayerDataManager().loadAllForLeaderboard();
            cache.set(all);
            lastRefresh = System.currentTimeMillis();
        });
    }

    public long getLastRefreshMillis() { return lastRefresh; }

    public List<PlayerData> getTop(Stat stat, int limit) {
        List<PlayerData> snapshot = new ArrayList<>(cache.get());
        Comparator<PlayerData> comparator = switch (stat) {
            case RANK -> Comparator
                .comparingInt((PlayerData d) -> plugin.getRankManager().getRankIndex(d.getCurrentRankId()))
                .thenComparingInt(PlayerData::getPrestige)
                .reversed();
            case PRESTIGE -> Comparator.comparingInt(PlayerData::getPrestige).reversed();
            case PLAYTIME -> Comparator.comparingLong(PlayerData::getTotalPlaytime).reversed();
            case KILLS -> Comparator.comparingInt(PlayerData::getPlayerKills).reversed();
            case DEATHS -> Comparator.comparingInt(PlayerData::getDeaths).reversed();
            case BLOCKS_BROKEN -> Comparator.comparingLong(PlayerData::getBlocksBroken).reversed();
            case BLOCKS_PLACED -> Comparator.comparingLong(PlayerData::getBlocksPlaced).reversed();
        };
        snapshot.sort(comparator);
        return snapshot.subList(0, Math.min(limit, snapshot.size()));
    }

    public static Stat parseStat(String input) {
        if (input == null) return Stat.RANK;
        return switch (input.toLowerCase()) {
            case "prestige" -> Stat.PRESTIGE;
            case "playtime" -> Stat.PLAYTIME;
            case "kills" -> Stat.KILLS;
            case "deaths" -> Stat.DEATHS;
            case "blocksbroken", "blocks_broken" -> Stat.BLOCKS_BROKEN;
            case "blocksplaced", "blocks_placed" -> Stat.BLOCKS_PLACED;
            default -> Stat.RANK;
        };
    }
}
