package com.kazumiii.rankupplus.managers;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.models.PlayerData;
import com.kazumiii.rankupplus.models.Rank;
import com.kazumiii.rankupplus.models.RankReward;
import com.kazumiii.rankupplus.utils.ColorUtils;
import com.kazumiii.rankupplus.utils.EffectsUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

public class RankUpManager {

    public enum RankUpResult {
        SUCCESS,
        ALREADY_MAX,
        NOT_ENOUGH_MONEY,
        REQUIREMENTS_NOT_MET,
        ON_COOLDOWN,
        ERROR
    }

    private final RankUpPlus plugin;

    public RankUpManager(RankUpPlus plugin) {
        this.plugin = plugin;
    }

    public RankUpResult attemptRankUp(Player player, boolean bypassCost, boolean bypassRequirements) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return RankUpResult.ERROR;

        String currentRankId = data.getCurrentRankId();
        Rank currentRank = plugin.getRankManager().getRank(currentRankId);

        if (currentRank == null || plugin.getRankManager().isMaxRank(currentRankId)) {
            // Check if prestige is possible
            if (plugin.getPrestigeManager().canPrestige(player)) {
                plugin.getPrestigeManager().prestige(player);
                return RankUpResult.SUCCESS;
            }
            return RankUpResult.ALREADY_MAX;
        }

        // Cooldown check (bypassed by either the dedicated cooldown permission or the cost-bypass permission)
        long cooldown = plugin.getConfigManager().getRankupCooldown();
        boolean bypassCooldown = bypassCost || player.hasPermission("rankupplus.bypass.cooldown");
        if (cooldown > 0 && !bypassCooldown) {
            long timeSince = (System.currentTimeMillis() - data.getLastRankupTime()) / 1000;
            if (timeSince < cooldown) {
                // Send cooldown message from caller
                return RankUpResult.ON_COOLDOWN;
            }
        }

        Rank nextRank = plugin.getRankManager().getNextRank(currentRankId);
        if (nextRank == null) return RankUpResult.ALREADY_MAX;

        // Cost check (with any active cost-discount booster applied)
        double effectiveCost = plugin.getBoosterManager().applyCostMultiplier(
            data, plugin.getRankManager().getEffectiveCost(currentRank, data.getPrestige()));
        if (!bypassCost && !plugin.getEconomyManager().has(player, effectiveCost)) {
            return RankUpResult.NOT_ENOUGH_MONEY;
        }

        // Requirements check
        if (!bypassRequirements) {
            List<String> unmet = plugin.getRequirementManager().getUnmetRequirements(player, nextRank);
            if (!unmet.isEmpty()) {
                return RankUpResult.REQUIREMENTS_NOT_MET;
            }
        }

        // All checks passed — execute rankup
        executeRankUp(player, data, currentRank, nextRank, effectiveCost, bypassCost);
        return RankUpResult.SUCCESS;
    }

    private void executeRankUp(Player player, PlayerData data, Rank fromRank, Rank toRank,
                                double cost, boolean bypassCost) {
        // Deduct cost
        if (!bypassCost) {
            plugin.getEconomyManager().withdraw(player, cost);
        }

        // Update player data
        data.setCurrentRankId(toRank.getId());
        data.setLastRankupTime(System.currentTimeMillis());
        plugin.getPlayerDataManager().save(data);

        // LuckPerms group assignment (real API, with console-command fallback)
        plugin.getLuckPermsHook().setGroup(player, fromRank.getPermission(), toRank.getPermission());

        // Tab-list team + sidebar refresh
        plugin.getScoreboardManager().updatePlayerTeam(player);
        plugin.getScoreboardManager().refreshSidebar(player);

        // Run server commands (these belong to the rank being reached, not the one being left)
        for (String cmd : toRank.getCommands()) {
            String parsed = cmd.replace("{player}", player.getName())
                               .replace("{rank}", toRank.getId())
                               .replace("{rank_display}", toRank.getDisplay());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }

        // Run player commands (also belong to the rank being reached)
        for (String cmd : toRank.getPlayerCommands()) {
            String parsed = cmd.replace("{player}", player.getName())
                               .replace("{rank}", toRank.getId())
                               .replace("{rank_display}", toRank.getDisplay());
            Bukkit.dispatchCommand(player, parsed);
        }

        // Give rewards for the rank being reached (boosted by an active REWARDS booster, if any)
        for (RankReward reward : toRank.getRewards()) {
            org.bukkit.inventory.ItemStack stack = reward.toItemStack();
            stack.setAmount(plugin.getBoosterManager().applyRewardsMultiplier(data, stack.getAmount()));
            player.getInventory().addItem(stack).forEach((slot, item) ->
                player.getWorld().dropItemNaturally(player.getLocation(), item));
        }

        // Visual/sound effects
        EffectsUtil.playRankupEffects(plugin, player);

        // Title
        if (plugin.getConfigManager().isShowTitle()) {
            String title = ColorUtils.color(
                plugin.getConfigManager().getRankupTitle()
                    .replace("{rank_color}", toRank.getColor())
                    .replace("{rank_display}", toRank.getDisplay())
            );
            String subtitle = ColorUtils.color(
                plugin.getConfigManager().getRankupSubtitle()
                    .replace("{rank_color}", toRank.getColor())
                    .replace("{rank_display}", toRank.getDisplay())
            );
            player.sendTitle(title, subtitle,
                plugin.getConfigManager().getTitleFadeIn(),
                plugin.getConfigManager().getTitleStay(),
                plugin.getConfigManager().getTitleFadeOut());
        }

        // Chat message
        plugin.getMessageManager().sendRaw(player,
            plugin.getMessageManager().getRawMessage("rankup-success")
                .replace("{rank_color}", toRank.getColor())
                .replace("{rank_display}", toRank.getDisplay())
        );

        // Broadcast
        if (plugin.getConfigManager().isBroadcastRankup()) {
            String broadcast = ColorUtils.color(
                plugin.getConfigManager().getBroadcastFormat()
                    .replace("{player}", player.getName())
                    .replace("{rank_color}", toRank.getColor())
                    .replace("{rank_display}", toRank.getDisplay())
            );
            Bukkit.broadcastMessage(broadcast);
        }

        // Notify admins
        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (admin.hasPermission("rankupplus.notify.admin") && !admin.equals(player)) {
                admin.sendMessage(ColorUtils.color(
                    "&8[&bRankUp&3+&8] &e" + player.getName() + " &7ranked up to " + toRank.getColoredDisplay()
                ));
            }
        }
    }

    public long getRemainingCooldown(PlayerData data) {
        long cooldown = plugin.getConfigManager().getRankupCooldown();
        if (cooldown <= 0) return 0;
        long elapsed = (System.currentTimeMillis() - data.getLastRankupTime()) / 1000;
        return Math.max(0, cooldown - elapsed);
    }
}
