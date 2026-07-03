package com.kazumiii.rankupplus.commands;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.managers.MessageManager;
import com.kazumiii.rankupplus.managers.RankUpManager;
import com.kazumiii.rankupplus.models.PlayerData;
import com.kazumiii.rankupplus.models.Rank;
import com.kazumiii.rankupplus.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RankUpCommand implements CommandExecutor, TabCompleter {

    private final RankUpPlus plugin;

    public RankUpCommand(RankUpPlus plugin) {
        this.plugin = plugin;
    }

    private RankUpManager rankUpManager() {
        return plugin.getRankUpManager();
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

        if (args.length >= 1 && (args[0].equalsIgnoreCase("scoreboard") || args[0].equalsIgnoreCase("sb"))) {
            boolean nowOn = plugin.getScoreboardManager().toggleSidebar(player);
            player.sendMessage(ColorUtils.color(nowOn
                ? "&aScoreboard enabled."
                : "&7Scoreboard disabled."));
            return true;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(ColorUtils.color("&cYour data has not loaded yet. Please wait."));
            return true;
        }

        // Cooldown check
        long remaining = rankUpManager().getRemainingCooldown(data);
        if (remaining > 0 && !player.hasPermission("rankupplus.bypass.cost")) {
            plugin.getMessageManager().send(player, "on-cooldown",
                "{time}", MessageManager.formatTime(remaining));
            return true;
        }

        // Check if using confirmation GUI
        if (plugin.getConfigManager().isConfirmRankup()) {
            Rank nextRank = plugin.getRankManager().getNextRank(data.getCurrentRankId());
            if (nextRank == null && !plugin.getPrestigeManager().canPrestige(player)) {
                plugin.getMessageManager().send(player, "already-max-rank");
                return true;
            }
            if (nextRank != null) {
                player.openInventory(plugin.getGuiManager().buildConfirmGui(player, nextRank));
            } else {
                // Open prestige confirm
                doRankUp(player, data);
            }
            return true;
        }

        doRankUp(player, data);
        return true;
    }

    private void doRankUp(Player player, PlayerData data) {
        boolean bypassCost = player.hasPermission("rankupplus.bypass.cost");
        boolean bypassReqs = player.hasPermission("rankupplus.bypass.requirements");

        RankUpManager.RankUpResult result = rankUpManager().attemptRankUp(player, bypassCost, bypassReqs);
        handleResult(player, result, data);
    }

    private void handleResult(Player player, RankUpManager.RankUpResult result, PlayerData data) {
        switch (result) {
            case ALREADY_MAX -> plugin.getMessageManager().send(player, "already-max-rank");
            case NOT_ENOUGH_MONEY -> {
                Rank currentRank = plugin.getRankManager().getRank(data.getCurrentRankId());
                double cost = currentRank != null
                    ? plugin.getRankManager().getEffectiveCost(currentRank, data.getPrestige()) : 0;
                double balance = plugin.getEconomyManager().getBalance(player);
                plugin.getMessageManager().send(player, "not-enough-money",
                    "{cost}", MessageManager.formatMoney(cost - balance),
                    "{balance}", MessageManager.formatMoney(balance));
            }
            case REQUIREMENTS_NOT_MET -> {
                plugin.getMessageManager().send(player, "requirements-not-met");
                Rank nextRank = plugin.getRankManager().getNextRank(data.getCurrentRankId());
                if (nextRank != null) {
                    List<String> unmet = plugin.getRequirementManager().getUnmetRequirements(player, nextRank);
                    for (String req : unmet) {
                        player.sendMessage(ColorUtils.color("  &c✖ " + req));
                    }
                }
            }
            case ON_COOLDOWN -> plugin.getMessageManager().send(player, "on-cooldown",
                "{time}", MessageManager.formatTime(rankUpManager().getRemainingCooldown(data)));
            case ERROR -> player.sendMessage(ColorUtils.color("&cAn error occurred. Please contact an admin."));
            case SUCCESS -> {} // Handled in RankUpManager
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("scoreboard").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(java.util.stream.Collectors.toList());
        }
        return new ArrayList<>();
    }
}
