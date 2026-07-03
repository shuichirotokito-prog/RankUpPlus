package com.kazumiii.rankupplus.managers;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.models.PlayerData;
import com.kazumiii.rankupplus.utils.ColorUtils;
import com.kazumiii.rankupplus.utils.EffectsUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public class PrestigeManager {

    private final RankUpPlus plugin;

    public PrestigeManager(RankUpPlus plugin) {
        this.plugin = plugin;
    }

    public boolean canPrestige(Player player) {
        if (!plugin.getConfigManager().isPrestigeEnabled()) return false;
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return false;
        if (data.getPrestige() >= plugin.getConfigManager().getMaxPrestige()) return false;
        return plugin.getRankManager().isMaxRank(data.getCurrentRankId());
    }

    public boolean prestige(Player player) {
        if (!canPrestige(player)) return false;

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        int newPrestige = data.getPrestige() + 1;
        String oldRankId = data.getCurrentRankId();

        // Reset rank
        if (plugin.getConfigManager().isPrestigeResetRank()) {
            com.kazumiii.rankupplus.models.Rank oldRank = plugin.getRankManager().getRank(oldRankId);
            com.kazumiii.rankupplus.models.Rank firstRank = plugin.getRankManager().getFirstRank();
            data.setCurrentRankId(firstRank.getId());
            plugin.getLuckPermsHook().setGroup(player, oldRank != null ? oldRank.getPermission() : null, firstRank.getPermission());
            plugin.getScoreboardManager().updatePlayerTeam(player);
        }

        // Remove balance unless keeping it
        if (!plugin.getConfigManager().isPrestigeKeepBalance()) {
            double balance = plugin.getEconomyManager().getBalance(player);
            plugin.getEconomyManager().withdraw(player, balance);
        }

        // Optionally reset progress stats so rank requirements must be earned again each prestige
        if (plugin.getConfigManager().isPrestigeResetStats()) {
            data.setTotalPlaytime(0);
            data.setPlayerKills(0);
            data.setDeaths(0);
            data.setBlocksBroken(0);
            data.setBlocksPlaced(0);
        }

        data.incrementPrestige();
        data.setLastRankupTime(System.currentTimeMillis());

        // Run prestige commands
        runPrestigeCommands(player, newPrestige);

        // Effects
        EffectsUtil.playRankupEffects(plugin, player);
        EffectsUtil.launchFirework(player.getLocation(), java.awt.Color.MAGENTA);

        String msg = ColorUtils.color(
            plugin.getMessageManager().getMessage("prestige-success")
                .replace("{prestige}", String.valueOf(newPrestige))
        );
        player.sendMessage(msg);

        return true;
    }

    private void runPrestigeCommands(Player player, int prestige) {
        ConfigurationSection rewardsSection = plugin.getConfigManager().getRanksConfig()
            .getConfigurationSection("prestige.rewards-per-prestige." + prestige);
        if (rewardsSection == null) return;

        List<String> commands = rewardsSection.getStringList("commands");
        for (String cmd : commands) {
            String parsed = cmd.replace("{player}", player.getName())
                               .replace("{prestige}", String.valueOf(prestige));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }

    public String getPrestigeDisplay(int prestige) {
        if (prestige <= 0) return "";
        return ColorUtils.color(
            plugin.getConfigManager().getPrestigeDisplayFormat()
                .replace("{prestige}", String.valueOf(prestige))
        );
    }
}
