package com.kazumiii.rankupplus.commands;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.models.PlayerData;
import com.kazumiii.rankupplus.models.Rank;
import com.kazumiii.rankupplus.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RankAdminCommand implements CommandExecutor, TabCompleter {

    private final RankUpPlus plugin;

    public RankAdminCommand(RankUpPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rankupplus.admin")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtils.color("&cUsage: /rankadmin set <player> <rank>"));
                    return true;
                }
                handleSet(sender, args[1], args[2]);
            }
            case "reset" -> {
                if (args.length < 2) {
                    sender.sendMessage(ColorUtils.color("&cUsage: /rankadmin reset <player>"));
                    return true;
                }
                handleReset(sender, args[1]);
            }
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(ColorUtils.color("&cUsage: /rankadmin info <player>"));
                    return true;
                }
                handleInfo(sender, args[1]);
            }
            case "reload" -> {
                plugin.reload();
                plugin.getMessageManager().send(sender, "admin-reload");
            }
            case "prestige" -> {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtils.color("&cUsage: /rankadmin prestige <player> <set|reset> [level]"));
                    return true;
                }
                handlePrestige(sender, args);
            }
            case "give" -> {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtils.color("&cUsage: /rankadmin give <player> <rank>"));
                    sender.sendMessage(ColorUtils.color("&7Gives the player the item rewards configured for that rank."));
                    return true;
                }
                handleGive(sender, args[1], args[2]);
            }
            case "booster" -> {
                if (args.length < 5) {
                    sender.sendMessage(ColorUtils.color("&cUsage: /rankadmin booster <player> <cost|rewards> <multiplier> <minutes>"));
                    sender.sendMessage(ColorUtils.color("&7cost multiplier < 1 is a discount (0.5 = 50% off)."));
                    sender.sendMessage(ColorUtils.color("&7rewards multiplier > 1 is a bonus (2 = double rewards)."));
                    return true;
                }
                handleBooster(sender, args);
            }
            case "edit" -> {
                if (!(sender instanceof Player editor)) {
                    sender.sendMessage(ColorUtils.color("&cThis command can only be used by a player."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtils.color("&cUsage: /rankadmin edit <rank>"));
                    return true;
                }
                handleEdit(editor, args[1]);
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleSet(CommandSender sender, String playerName, String rankId) {
        Rank rank = plugin.getRankManager().getRank(rankId);
        if (rank == null) {
            plugin.getMessageManager().send(sender, "invalid-rank", "{rank}", rankId);
            return;
        }

        // Try online player first
        Player target = Bukkit.getPlayerExact(playerName);
        if (target != null) {
            PlayerData data = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
            if (data == null) {
                sender.sendMessage(ColorUtils.color("&cPlayer data not found."));
                return;
            }
            Rank oldRank = plugin.getRankManager().getRank(data.getCurrentRankId());
            data.setCurrentRankId(rank.getId());
            plugin.getPlayerDataManager().save(data);
            plugin.getLuckPermsHook().setGroup(target, oldRank != null ? oldRank.getPermission() : null, rank.getPermission());
            plugin.getScoreboardManager().updatePlayerTeam(target);
            plugin.getMessageManager().send(sender, "admin-set-rank",
                "{player}", playerName, "{rank}", rank.getColoredDisplay());
            target.sendMessage(ColorUtils.color("&bYour rank has been set to " + rank.getColoredDisplay() + " &bby an admin."));
        } else {
            sender.sendMessage(ColorUtils.color("&cPlayer &e" + playerName + " &cmust be online to set rank."));
        }
    }

    private void handleReset(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayerExact(playerName);
        PlayerData data = target != null
            ? plugin.getPlayerDataManager().getPlayerData(target.getUniqueId())
            : plugin.getPlayerDataManager().findByName(playerName);

        if (data == null) {
            plugin.getMessageManager().send(sender, "invalid-player", "{player}", playerName);
            return;
        }

        Rank oldRank = plugin.getRankManager().getRank(data.getCurrentRankId());
        Rank defaultRank = plugin.getRankManager().getRank("DEFAULT");
        data.setCurrentRankId("DEFAULT");
        data.setPrestige(0);
        plugin.getPlayerDataManager().save(data);

        if (target != null && defaultRank != null) {
            plugin.getLuckPermsHook().setGroup(target, oldRank != null ? oldRank.getPermission() : null, defaultRank.getPermission());
            plugin.getScoreboardManager().updatePlayerTeam(target);
        }

        plugin.getMessageManager().send(sender, "admin-reset-rank", "{player}", playerName);
        if (target != null) {
            target.sendMessage(ColorUtils.color("&cYour rank has been reset by an admin."));
        }
    }

    private void handleInfo(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayerExact(playerName);
        PlayerData data = target != null
            ? plugin.getPlayerDataManager().getPlayerData(target.getUniqueId())
            : plugin.getPlayerDataManager().findByName(playerName);

        if (data == null) {
            sender.sendMessage(ColorUtils.color("&cNo data found for &e" + playerName + "&c."));
            return;
        }

        Rank currentRank = plugin.getRankManager().getRank(data.getCurrentRankId());
        sender.sendMessage(ColorUtils.color("&8&m                              "));
        sender.sendMessage(ColorUtils.color("&b&lPlayer Info: &f" + playerName));
        sender.sendMessage(ColorUtils.color("&7Rank: &r" + (currentRank != null ? currentRank.getColoredDisplay() : data.getCurrentRankId())));
        sender.sendMessage(ColorUtils.color("&7Prestige: &5" + data.getPrestige()));
        sender.sendMessage(ColorUtils.color("&7Kills: &c" + data.getPlayerKills()));
        sender.sendMessage(ColorUtils.color("&7Deaths: &e" + data.getDeaths()));
        sender.sendMessage(ColorUtils.color("&7Blocks Broken: &b" + data.getBlocksBroken()));
        sender.sendMessage(ColorUtils.color("&7Blocks Placed: &b" + data.getBlocksPlaced()));
        sender.sendMessage(ColorUtils.color("&7Playtime: &b" + data.getTotalPlaytime() + " minutes"));
        if (target != null) {
            sender.sendMessage(ColorUtils.color("&7Balance: &e" + plugin.getEconomyManager().formatCurrency(
                plugin.getEconomyManager().getBalance(target))));
        }
        sender.sendMessage(ColorUtils.color("&8&m                              "));
    }

    private void handleGive(CommandSender sender, String playerName, String rankId) {
        Rank rank = plugin.getRankManager().getRank(rankId);
        if (rank == null) {
            plugin.getMessageManager().send(sender, "invalid-rank", "{rank}", rankId);
            return;
        }

        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage(ColorUtils.color("&cPlayer &e" + playerName + " &cmust be online to receive rewards."));
            return;
        }

        if (rank.getRewards().isEmpty()) {
            sender.sendMessage(ColorUtils.color("&cRank &e" + rank.getId() + " &chas no item rewards configured."));
            return;
        }

        for (com.kazumiii.rankupplus.models.RankReward reward : rank.getRewards()) {
            target.getInventory().addItem(reward.toItemStack()).forEach((slot, item) ->
                target.getWorld().dropItemNaturally(target.getLocation(), item));
        }

        sender.sendMessage(ColorUtils.color("&aGave &e" + playerName + " &athe rewards for rank " + rank.getColoredDisplay() + "&a."));
        target.sendMessage(ColorUtils.color("&bYou received the rewards for rank " + rank.getColoredDisplay() + " &bfrom an admin."));
    }

    private void handleBooster(CommandSender sender, String[] args) {
        String playerName = args[1];
        String typeArg = args[2].toUpperCase();
        if (!typeArg.equals("COST") && !typeArg.equals("REWARDS")) {
            sender.sendMessage(ColorUtils.color("&cBooster type must be &ecost &cor &erewards&c."));
            return;
        }

        double multiplier;
        long minutes;
        try {
            multiplier = Double.parseDouble(args[3]);
            minutes = Long.parseLong(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtils.color("&cMultiplier and minutes must both be numbers."));
            return;
        }

        Player target = Bukkit.getPlayerExact(playerName);
        PlayerData data = target != null
            ? plugin.getPlayerDataManager().getPlayerData(target.getUniqueId())
            : plugin.getPlayerDataManager().findByName(playerName);

        if (data == null) {
            plugin.getMessageManager().send(sender, "invalid-player", "{player}", playerName);
            return;
        }

        plugin.getBoosterManager().grant(data, typeArg, multiplier, minutes);
        String typeLabel = typeArg.equals("COST") ? "cost discount" : "reward boost";
        sender.sendMessage(ColorUtils.color("&aGranted &e" + playerName + " &aa " + typeLabel +
            " &7(x" + multiplier + ") &afor &e" + minutes + " &aminutes."));
        if (target != null) {
            target.sendMessage(ColorUtils.color("&8[&bRankUp&3+&8] &aYou received a " + typeLabel +
                " booster &7(x" + multiplier + ") &afor &e" + minutes + " &aminutes!"));
        }
    }

    private void handleEdit(Player editor, String rankId) {
        Rank rank = plugin.getRankManager().getRank(rankId);
        if (rank == null) {
            plugin.getMessageManager().send(editor, "invalid-rank", "{rank}", rankId);
            return;
        }
        editor.openInventory(plugin.getGuiManager().buildRankEditorGui(rank));
    }

    private void handlePrestige(CommandSender sender, String[] args) {
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ColorUtils.color("&cPlayer must be online."));
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
        if (data == null) return;

        if (args[2].equalsIgnoreCase("set") && args.length >= 4) {
            int level;
            try {
                level = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ColorUtils.color("&c'" + args[3] + "' is not a valid number."));
                return;
            }
            level = Math.max(0, Math.min(level, plugin.getConfigManager().getMaxPrestige()));
            data.setPrestige(level);
            plugin.getPlayerDataManager().save(data);
            sender.sendMessage(ColorUtils.color("&aSet " + args[1] + "'s prestige to &5" + level));
        } else if (args[2].equalsIgnoreCase("reset")) {
            data.setPrestige(0);
            plugin.getPlayerDataManager().save(data);
            sender.sendMessage(ColorUtils.color("&aReset " + args[1] + "'s prestige."));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtils.color("&8&m        &8[ &b&lRankAdmin &8] &8&m        "));
        sender.sendMessage(ColorUtils.color("&b/rankadmin set <player> <rank> &7- Set player rank"));
        sender.sendMessage(ColorUtils.color("&b/rankadmin reset <player> &7- Reset player rank"));
        sender.sendMessage(ColorUtils.color("&b/rankadmin give <player> <rank> &7- Give rank's item rewards"));
        sender.sendMessage(ColorUtils.color("&b/rankadmin info <player> &7- View player data"));
        sender.sendMessage(ColorUtils.color("&b/rankadmin prestige <player> set/reset [level] &7- Manage prestige"));
        sender.sendMessage(ColorUtils.color("&b/rankadmin booster <player> <cost|rewards> <mult> <mins> &7- Grant a booster"));
        sender.sendMessage(ColorUtils.color("&b/rankadmin edit <rank> &7- Open the rank editor GUI"));
        sender.sendMessage(ColorUtils.color("&b/rankadmin reload &7- Reload config"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("rankupplus.admin")) return new ArrayList<>();

        if (args.length == 1) {
            return Arrays.asList("set", "reset", "info", "reload", "prestige", "give", "booster", "edit").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("edit")) {
            return plugin.getRankManager().getRankOrder().stream()
                .filter(r -> r.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("give"))) {
            return plugin.getRankManager().getRankOrder().stream()
                .filter(r -> r.toLowerCase().startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("booster")) {
            return Arrays.asList("cost", "rewards").stream()
                .filter(s -> s.startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
