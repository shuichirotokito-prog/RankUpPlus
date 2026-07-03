package com.kazumiii.rankupplus.placeholders;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.managers.MessageManager;
import com.kazumiii.rankupplus.models.PlayerData;
import com.kazumiii.rankupplus.models.Rank;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class RankUpPlusExpansion extends PlaceholderExpansion {

    private final RankUpPlus plugin;

    public RankUpPlusExpansion(RankUpPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "rankupplus"; }

    @Override
    public @NotNull String getAuthor() { return "Kazumiii"; }

    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        PlayerData data = plugin.getPlayerDataManager().getOfflineData(player.getUniqueId(), player.getName());
        if (data == null) return "N/A";

        Rank currentRank = plugin.getRankManager().getRank(data.getCurrentRankId());
        Rank nextRank = plugin.getRankManager().getNextRank(data.getCurrentRankId());

        return switch (params.toLowerCase()) {
            case "rank" -> currentRank != null ? currentRank.getColoredDisplay() : "Unknown";
            case "rank_id" -> data.getCurrentRankId();
            case "rank_display" -> currentRank != null ? currentRank.getDisplay() : "Unknown";
            case "rank_color" -> currentRank != null ? "&" + currentRank.getColor() : "";
            case "rank_index" -> String.valueOf(plugin.getRankManager().getRankIndex(data.getCurrentRankId()));
            case "total_ranks" -> String.valueOf(plugin.getRankManager().getTotalRanks());
            case "next_rank" -> nextRank != null ? nextRank.getColoredDisplay() : "MAX";
            case "next_rank_id" -> nextRank != null ? nextRank.getId() : "MAX";
            case "next_rank_cost" -> {
                if (currentRank == null) yield "N/A";
                double cost = plugin.getBoosterManager().applyCostMultiplier(data,
                    plugin.getRankManager().getEffectiveCost(currentRank, data.getPrestige()));
                yield MessageManager.formatMoney(cost);
            }
            case "prestige" -> String.valueOf(data.getPrestige());
            case "prestige_display" -> plugin.getPrestigeManager().getPrestigeDisplay(data.getPrestige());
            case "kills" -> String.valueOf(data.getPlayerKills());
            case "deaths" -> String.valueOf(data.getDeaths());
            case "blocks_broken" -> String.valueOf(data.getBlocksBroken());
            case "blocks_placed" -> String.valueOf(data.getBlocksPlaced());
            case "balance" -> player.isOnline() && player.getPlayer() != null
                ? plugin.getEconomyManager().formatCurrency(plugin.getEconomyManager().getBalance(player.getPlayer()))
                : "N/A";
            case "booster" -> plugin.getBoosterManager().getDisplay(data);
            case "booster_active" -> String.valueOf(plugin.getBoosterManager().isActive(data));
            case "online" -> String.valueOf(plugin.getServer().getOnlinePlayers().size());
            case "max_players" -> String.valueOf(plugin.getServer().getMaxPlayers());
            case "playtime" -> String.valueOf(data.getTotalPlaytime());
            case "playtime_formatted" -> MessageManager.formatTime(data.getTotalPlaytime() * 60);
            case "is_max_rank" -> String.valueOf(plugin.getRankManager().isMaxRank(data.getCurrentRankId()));
            case "progress_percent" -> {
                int total = plugin.getRankManager().getTotalRanks();
                int idx = plugin.getRankManager().getRankIndex(data.getCurrentRankId());
                if (total <= 1) yield "100";
                yield String.valueOf((int) ((double) idx / (total - 1) * 100));
            }
            default -> null;
        };
    }
}
