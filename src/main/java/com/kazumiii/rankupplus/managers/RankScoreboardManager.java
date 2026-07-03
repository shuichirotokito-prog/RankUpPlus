package com.kazumiii.rankupplus.managers;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.models.PlayerData;
import com.kazumiii.rankupplus.models.Rank;
import com.kazumiii.rankupplus.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages two related visual features that both rely on each player having
 * their own Scoreboard object:
 *
 *  - A personalized sidebar showing rank/prestige, balance/next cost, stats,
 *    and server info, refreshed periodically.
 *  - Tab-list sorting and colored rank prefixes, via one scoreboard Team per
 *    rank (named with a zero-padded rank-index prefix so vanilla's tab-list
 *    sort, which groups by team name, naturally orders players by rank).
 *
 * Because tab-list team membership is broadcast per the Scoreboard object a
 * client currently has assigned (which we're replacing per-player so each
 * player can have their own personalized sidebar), the same set of rank teams
 * is replicated onto every online player's individual Scoreboard and kept in
 * sync on join/quit/rank-change.
 */
public class RankScoreboardManager {

    private static final String OBJECTIVE_NAME = "rup_sb";

    private final RankUpPlus plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final Map<UUID, Boolean> sidebarToggle = new HashMap<>();

    public RankScoreboardManager(RankUpPlus plugin) {
        this.plugin = plugin;
    }

    // ---------------- Join / quit / rank-change hooks ----------------

    public void setupPlayer(Player player) {
        boolean tablistOn = plugin.getConfigManager().isTablistEnabled();
        boolean scoreboardOn = plugin.getConfigManager().isScoreboardEnabled();
        if (!tablistOn && !scoreboardOn) return;

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        if (tablistOn) {
            setupTeamsOnBoard(board);
            populateAllEntries(board);
        }

        boards.put(player.getUniqueId(), board);
        player.setScoreboard(board);

        if (tablistOn) {
            // Make sure every other online player's own board also knows about this new player.
            for (Map.Entry<UUID, Scoreboard> entry : boards.entrySet()) {
                if (entry.getKey().equals(player.getUniqueId())) continue;
                addPlayerToCorrectTeam(entry.getValue(), player);
            }
        }

        boolean sidebarOn = sidebarToggle.computeIfAbsent(player.getUniqueId(),
            k -> plugin.getConfigManager().isScoreboardDefaultOn());
        if (scoreboardOn && sidebarOn) {
            refreshSidebar(player);
        }
    }

    public void teardownPlayer(UUID uuid, String name) {
        boards.remove(uuid);
        sidebarToggle.remove(uuid);
        for (Scoreboard board : boards.values()) {
            for (Team team : board.getTeams()) {
                if (team.hasEntry(name)) {
                    team.removeEntry(name);
                }
            }
        }
    }

    /** Call whenever a player's rank changes (rankup, prestige, admin set/reset). */
    public void updatePlayerTeam(Player player) {
        if (!plugin.getConfigManager().isTablistEnabled()) return;
        for (Scoreboard board : boards.values()) {
            addPlayerToCorrectTeam(board, player);
        }
    }

    // ---------------- Sidebar toggle ----------------

    public boolean toggleSidebar(Player player) {
        boolean current = sidebarToggle.getOrDefault(player.getUniqueId(),
            plugin.getConfigManager().isScoreboardDefaultOn());
        boolean newState = !current;
        sidebarToggle.put(player.getUniqueId(), newState);
        if (newState) {
            refreshSidebar(player);
        } else {
            Scoreboard board = boards.get(player.getUniqueId());
            if (board != null) {
                Objective obj = board.getObjective(OBJECTIVE_NAME);
                if (obj != null) obj.unregister();
            }
        }
        return newState;
    }

    public void refreshAll() {
        if (!plugin.getConfigManager().isScoreboardEnabled()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean on = sidebarToggle.getOrDefault(player.getUniqueId(),
                plugin.getConfigManager().isScoreboardDefaultOn());
            if (on) refreshSidebar(player);
        }
    }

    public void refreshSidebar(Player player) {
        if (!plugin.getConfigManager().isScoreboardEnabled()) return;
        Scoreboard board = boards.get(player.getUniqueId());
        if (board == null) return;
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        Objective old = board.getObjective(OBJECTIVE_NAME);
        if (old != null) old.unregister();

        String title = ColorUtils.color(plugin.getConfigManager().getScoreboardTitle());
        if (title.length() > 32) title = title.substring(0, 32);
        @SuppressWarnings("deprecation")
        Objective obj = board.registerNewObjective(OBJECTIVE_NAME, "dummy", title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> rawLines = plugin.getConfigManager().getScoreboardLines();
        int score = rawLines.size();
        Set<String> used = new HashSet<>();
        for (String raw : rawLines) {
            String line = ColorUtils.color(resolveLine(raw, player, data));
            String entry = line.isEmpty() ? " " : line;
            while (!used.add(entry)) entry = entry + " ";
            if (entry.length() > 128) entry = entry.substring(0, 128);
            obj.getScore(entry).setScore(score--);
        }
    }

    private String resolveLine(String raw, Player player, PlayerData data) {
        Rank currentRank = plugin.getRankManager().getRank(data.getCurrentRankId());
        Rank nextRank = plugin.getRankManager().getNextRank(data.getCurrentRankId());

        String nextCostStr;
        if (nextRank == null || currentRank == null) {
            nextCostStr = "MAX";
        } else {
            double cost = plugin.getBoosterManager().applyCostMultiplier(data,
                plugin.getRankManager().getEffectiveCost(currentRank, data.getPrestige()));
            nextCostStr = plugin.getEconomyManager().formatCurrency(cost);
        }

        // Apply RankUpPlus's own placeholders first, then pass through PapiBridge
        // so any %other_plugin_placeholder% values in the line also get resolved.
        String withFields = raw
            .replace("{player}", player.getName())
            .replace("{rank_color}", currentRank != null ? currentRank.getColor() : "7")
            .replace("{rank_display}", currentRank != null ? currentRank.getDisplay() : data.getCurrentRankId())
            .replace("{prestige}", String.valueOf(data.getPrestige()))
            .replace("{prestige_display}", plugin.getPrestigeManager().getPrestigeDisplay(data.getPrestige()))
            .replace("{balance}", plugin.getEconomyManager().formatCurrency(plugin.getEconomyManager().getBalance(player)))
            .replace("{next_cost}", nextCostStr)
            .replace("{playtime}", String.valueOf(data.getTotalPlaytime()))
            .replace("{kills}", String.valueOf(data.getPlayerKills()))
            .replace("{deaths}", String.valueOf(data.getDeaths()))
            .replace("{blocks_broken}", String.valueOf(data.getBlocksBroken()))
            .replace("{blocks_placed}", String.valueOf(data.getBlocksPlaced()))
            .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
            .replace("{max_players}", String.valueOf(Bukkit.getMaxPlayers()))
            .replace("{booster}", plugin.getBoosterManager().getDisplay(data));

        return plugin.getPapiBridge().apply(player, withFields);
    }

    // ---------------- Tab-list teams ----------------

    private void setupTeamsOnBoard(Scoreboard board) {
        List<String> order = plugin.getRankManager().getRankOrder();
        boolean showPrefix = plugin.getConfigManager().isTablistShowRankPrefix();
        for (int i = 0; i < order.size(); i++) {
            String rankId = order.get(i);
            Rank rank = plugin.getRankManager().getRank(rankId);
            if (rank == null) continue;

            String teamName = teamName(i, rankId);
            Team team = board.getTeam(teamName);
            if (team == null) team = board.registerNewTeam(teamName);

            if (showPrefix) {
                String prefix = ColorUtils.color(
                    plugin.getConfigManager().getTablistPrefixFormat()
                        .replace("{rank_color}", rank.getColor())
                        .replace("{rank_display}", rank.getDisplay())
                );
                if (prefix.length() > 64) prefix = prefix.substring(0, 64);
                team.setPrefix(prefix);

                try {
                    if (rank.getColor() != null && !rank.getColor().isEmpty()) {
                        ChatColor cc = ChatColor.getByChar(rank.getColor().charAt(0));
                        if (cc != null) team.setColor(cc);
                    }
                } catch (Exception ignored) {
                    // Invalid color char in config — leave the team's default color alone.
                }
            }

            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        }
    }

    private void populateAllEntries(Scoreboard board) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            addPlayerToCorrectTeam(board, player);
        }
    }

    private void addPlayerToCorrectTeam(Scoreboard board, Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        String rankId = data != null ? data.getCurrentRankId() : "DEFAULT";
        int index = plugin.getRankManager().getRankIndex(rankId);
        if (index < 0) index = 0;
        Team team = board.getTeam(teamName(index, rankId));
        if (team == null) return; // Shouldn't happen — teams are set up for every known rank.
        team.addEntry(player.getName());
    }

    /**
     * When sort-by-rank is on, team names are zero-padded by rank index so vanilla's
     * tab-list grouping (alphabetical by team name) naturally orders players by rank.
     * When off, teams are just named by rank id directly (no guaranteed order) — still
     * useful for colored prefixes without imposing a specific tab-list order.
     */
    private String teamName(int index, String rankId) {
        if (plugin.getConfigManager().isTablistSortByRank()) {
            String safeId = rankId.length() > 13 ? rankId.substring(0, 13) : rankId;
            return String.format("%02d_%s", index, safeId);
        }
        return rankId.length() > 16 ? rankId.substring(0, 16) : rankId;
    }
}
