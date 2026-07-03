package com.kazumiii.rankupplus.commands;

import com.kazumiii.rankupplus.RankUpPlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RankUpGuiCommand implements CommandExecutor {

    private final RankUpPlus plugin;

    public RankUpGuiCommand(RankUpPlus plugin) {
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

        plugin.getPlayerDataManager().getOrCreate(player.getUniqueId(), player.getName());
        player.openInventory(plugin.getGuiManager().buildRankUpGui(player));
        return true;
    }
}
