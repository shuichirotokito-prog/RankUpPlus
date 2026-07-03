package com.kazumiii.rankupplus.commands;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.managers.LeaderboardManager;
import com.kazumiii.rankupplus.managers.MessageManager;
import com.kazumiii.rankupplus.models.PlayerData;
import com.kazumiii.rankupplus.models.Rank;
import com.kazumiii.rankupplus.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RankTopCommand implements CommandExecutor, TabCompleter {

    private final RankUpPlus plugin;

    public RankTopCommand(RankUpPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rankupplus.use")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return true;
        }

        LeaderboardManager.Stat stat = args.length >= 1
            ? LeaderboardManager.parseStat(args[0])
            : LeaderboardManager.Stat.RANK;

        int limit = plugin.getConfigManager().getLeaderboardSize();
        List<PlayerData> top = plugin.getLeaderboardManager().getTop(stat, limit);

        sender.sendMessage(ColorUtils.color("&8&m        &8[ &b&lTop " + statLabel(stat) + " &8] &8&m        "));
        if (top.isEmpty()) {
            sender.sendMessage(ColorUtils.color("&7No data yet — check back in a few minutes."));
            return true;
        }

        int pos = 1;
        for (PlayerData data : top) {
            sender.sendMessage(ColorUtils.color(formatLine(pos, data, stat)));
            pos++;
        }

        long lastRefresh = plugin.getLeaderboardManager().getLastRefreshMillis();
        if (lastRefresh > 0) {
            long ageSeconds = (System.currentTimeMillis() - lastRefresh) / 1000;
            sender.sendMessage(ColorUtils.color("&8Updated " + MessageManager.formatTime(ageSeconds) + " ago"));
        }
        return true;
    }

    private String statLabel(LeaderboardManager.Stat stat) {
        return switch (stat) {
            case RANK -> "Ranks";
            case PRESTIGE -> "Prestige";
            case PLAYTIME -> "Playtime";
            case KILLS -> "Kills";
            case DEATHS -> "Deaths";
            case BLOCKS_BROKEN -> "Blocks Broken";
            case BLOCKS_PLACED -> "Blocks Placed";
        };
    }

    private String formatLine(int pos, PlayerData data, LeaderboardManager.Stat stat) {
        String posColor = switch (pos) {
            case 1 -> "&6";
            case 2 -> "&7";
            case 3 -> "&c";
            default -> "&8";
        };

        String value = switch (stat) {
            case RANK -> {
                Rank rank = plugin.getRankManager().getRank(data.getCurrentRankId());
                String rankStr = rank != null ? rank.getColoredDisplay() : data.getCurrentRankId();
                yield data.getPrestige() > 0
                    ? rankStr + " " + plugin.getPrestigeManager().getPrestigeDisplay(data.getPrestige())
                    : rankStr;
            }
            case PRESTIGE -> "&5" + data.getPrestige();
            case PLAYTIME -> "&b" + data.getTotalPlaytime() + "m";
            case KILLS -> "&c" + data.getPlayerKills();
            case DEATHS -> "&e" + data.getDeaths();
            case BLOCKS_BROKEN -> "&b" + data.getBlocksBroken();
            case BLOCKS_PLACED -> "&b" + data.getBlocksPlaced();
        };

        String name = data.getName() != null ? data.getName() : "Unknown";
        return posColor + "#" + pos + " &f" + name + " &8» " + value;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("rank", "prestige", "playtime", "kills", "deaths", "blocksbroken", "blocksplaced")
                .stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
