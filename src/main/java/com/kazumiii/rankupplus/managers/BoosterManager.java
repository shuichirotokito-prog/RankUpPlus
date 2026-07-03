package com.kazumiii.rankupplus.managers;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.models.PlayerData;
import com.kazumiii.rankupplus.utils.ColorUtils;
import org.bukkit.entity.Player;

/**
 * Manages temporary per-player boosters:
 *  - COST boosters reduce rankup cost (multiplier should be < 1, e.g. 0.5 = 50% off)
 *  - REWARDS boosters scale item-reward quantities (multiplier should be > 1, e.g. 2.0 = double)
 *
 * Boosters are granted by admins (see /rankadmin booster) and persist across relogs
 * and restarts via fields on PlayerData.
 */
public class BoosterManager {

    public static final String TYPE_COST = "COST";
    public static final String TYPE_REWARDS = "REWARDS";

    private final RankUpPlus plugin;

    public BoosterManager(RankUpPlus plugin) {
        this.plugin = plugin;
    }

    public void grant(PlayerData data, String type, double multiplier, long minutes) {
        data.setBoosterType(type.toUpperCase());
        data.setBoosterMultiplier(multiplier);
        data.setBoosterExpiresAt(System.currentTimeMillis() + (minutes * 60_000L));
        plugin.getPlayerDataManager().save(data);
    }

    public void clear(PlayerData data) {
        data.clearBooster();
        plugin.getPlayerDataManager().save(data);
    }

    /** Applies an active COST booster (a discount) to a rankup cost, if one is active. */
    public double applyCostMultiplier(PlayerData data, double baseCost) {
        if (data.isBoosterActive(TYPE_COST)) {
            return Math.max(0, baseCost * data.getBoosterMultiplier());
        }
        return baseCost;
    }

    /** Applies an active REWARDS booster to an item reward amount, if one is active. */
    public int applyRewardsMultiplier(PlayerData data, int baseAmount) {
        if (data.isBoosterActive(TYPE_REWARDS)) {
            int boosted = (int) Math.round(baseAmount * data.getBoosterMultiplier());
            return Math.max(1, Math.min(2000, boosted));
        }
        return baseAmount;
    }

    public long getRemainingSeconds(PlayerData data) {
        if (data.getBoosterType() == null) return 0;
        long remaining = (data.getBoosterExpiresAt() - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    /** Human-readable summary of a player's active booster, or "None". */
    public String getDisplay(PlayerData data) {
        if (!isActive(data)) return "&7None";
        String typeLabel = TYPE_COST.equalsIgnoreCase(data.getBoosterType()) ? "Cost Discount" : "Reward Boost";
        return "&a" + typeLabel + " &7(&e" + MessageManager.formatTime(getRemainingSeconds(data)) + "&7 left)";
    }

    public boolean isActive(PlayerData data) {
        return data.getBoosterType() != null
            && data.getBoosterExpiresAt() > System.currentTimeMillis();
    }

    /**
     * Periodic sweep across online players: clears any booster that has expired
     * and notifies the player. Called from a repeating task.
     */
    public void tickExpirations() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
            if (data == null || data.getBoosterType() == null) continue;
            if (data.getBoosterExpiresAt() <= System.currentTimeMillis()) {
                String typeLabel = TYPE_COST.equalsIgnoreCase(data.getBoosterType()) ? "Cost discount" : "Reward boost";
                clear(data);
                player.sendMessage(ColorUtils.color("&8[&bRankUp&3+&8] &7Your " + typeLabel.toLowerCase() + " booster has expired."));
            }
        }
    }
}
