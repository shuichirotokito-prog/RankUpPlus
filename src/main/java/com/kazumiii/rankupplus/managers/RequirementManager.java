package com.kazumiii.rankupplus.managers;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.models.PlayerData;
import com.kazumiii.rankupplus.models.Rank;
import com.kazumiii.rankupplus.models.RankRequirement;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RequirementManager {

    private final RankUpPlus plugin;

    public RequirementManager(RankUpPlus plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks if a player meets all requirements for the given rank.
     * @return List of unmet requirement display strings, empty if all met.
     */
    public List<String> getUnmetRequirements(Player player, Rank targetRank) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        List<String> unmet = new ArrayList<>();

        if (data == null) return unmet;

        for (RankRequirement req : targetRank.getRequirements()) {
            if (!isMet(player, data, req)) {
                unmet.add(req.getDisplay());
            }
        }

        return unmet;
    }

    public boolean allMet(Player player, Rank targetRank) {
        return getUnmetRequirements(player, targetRank).isEmpty();
    }

    private boolean isMet(Player player, PlayerData data, RankRequirement req) {
        switch (req.getType()) {
            case PLAYTIME -> {
                return data.getTotalPlaytime() >= req.getValue();
            }
            case KILLS -> {
                return data.getPlayerKills() >= req.getValue();
            }
            case DEATHS -> {
                return data.getDeaths() >= req.getValue();
            }
            case BALANCE -> {
                return plugin.getEconomyManager().getBalance(player) >= req.getValue();
            }
            case LEVEL -> {
                return player.getLevel() >= req.getValue();
            }
            case BLOCKS_BROKEN -> {
                return data.getBlocksBroken() >= req.getValue();
            }
            case BLOCKS_PLACED -> {
                return data.getBlocksPlaced() >= req.getValue();
            }
            case PERMISSION -> {
                String node = req.getPermission();
                if (node == null || node.isBlank()) return false;
                return player.hasPermission(node);
            }
            case STATISTIC -> {
                if (req.getStatisticName() == null) return true;
                try {
                    org.bukkit.Statistic stat = org.bukkit.Statistic.valueOf(req.getStatisticName().toUpperCase());
                    return player.getStatistic(stat) >= (int) req.getValue();
                } catch (IllegalArgumentException e) {
                    return true; // Unknown statistic name in config; don't block rankups on a misconfiguration.
                }
            }
            default -> {
                return true;
            }
        }
    }

    /**
     * Returns a colored progress string for a requirement.
     */
    public String getRequirementProgress(Player player, PlayerData data, RankRequirement req) {
        if (data == null) return "&c" + req.getDisplay();
        boolean met = isMet(player, data, req);
        return (met ? "&a✔ " : "&c✖ ") + req.getDisplay();
    }
}
