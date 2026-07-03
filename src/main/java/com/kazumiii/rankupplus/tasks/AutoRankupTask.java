package com.kazumiii.rankupplus.tasks;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class AutoRankupTask extends BukkitRunnable {

    private final RankUpPlus plugin;

    public AutoRankupTask(RankUpPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("rankupplus.use")) continue;

            PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
            if (data == null) continue;
            if (plugin.getRankManager().isMaxRank(data.getCurrentRankId())) continue;

            boolean bypassCost = player.hasPermission("rankupplus.bypass.cost");
            boolean bypassReqs = player.hasPermission("rankupplus.bypass.requirements");

            plugin.getRankUpManager().attemptRankUp(player, bypassCost, bypassReqs);
            // Only SUCCESS matters; other results are silently ignored in auto mode
        }
    }
}
