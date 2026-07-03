package com.kazumiii.rankupplus.commands;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.models.PlayerData;
import com.kazumiii.rankupplus.models.Rank;
import com.kazumiii.rankupplus.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RanksCommand implements CommandExecutor {

    private final RankUpPlus plugin;

    public RanksCommand(RankUpPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return true;
        }

        if (!player.hasPermission("rankupplus.use")) {
            plugin.getMessageManager().send(player, "no-permission");
            return true;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        String currentRankId = data != null ? data.getCurrentRankId() : "DEFAULT";
        int currentIndex = plugin.getRankManager().getRankIndex(currentRankId);

        player.sendMessage(ColorUtils.color("&8&m        &8[ &b&lRankUp&3&l+ &8] &8&m        "));
        player.sendMessage(ColorUtils.color("&7All available ranks:"));
        player.sendMessage("");

        int index = 0;
        for (Rank rank : plugin.getRankManager().getAllRanks()) {
            String prefix;
            if (index < currentIndex) {
                prefix = "&a✔ ";
            } else if (index == currentIndex) {
                prefix = "&e➤ ";
            } else {
                prefix = "&8○ ";
            }

            double cost = plugin.getRankManager().getEffectiveCost(rank, data != null ? data.getPrestige() : 0);
            player.sendMessage(ColorUtils.color(
                prefix + rank.getColoredDisplay() + " &8| &7Cost: &e" +
                plugin.getEconomyManager().formatCurrency(cost)
            ));
            index++;
        }

        Rank currentRank = data != null ? plugin.getRankManager().getRank(currentRankId) : null;
        player.sendMessage("");
        player.sendMessage(ColorUtils.color("&7Your rank: &r" +
            (currentRank != null ? currentRank.getColoredDisplay() : "Unknown")));
        player.sendMessage(ColorUtils.color("&8&m                                    "));

        // Also open the GUI for a richer view with progress/requirements per rank
        player.openInventory(plugin.getGuiManager().buildRanksOverview(player));
        return true;
    }
}
