package com.kazumiii.rankupplus.managers;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.models.Rank;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class RankManager {

    private final RankUpPlus plugin;
    private final LinkedHashMap<String, Rank> ranks = new LinkedHashMap<>();
    private final List<String> rankOrder = new ArrayList<>();

    public RankManager(RankUpPlus plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        ranks.clear();
        rankOrder.clear();

        ConfigurationSection section = plugin.getConfigManager().getRanksConfig().getConfigurationSection("ranks");
        if (section == null) {
            plugin.getLogger().warning("No ranks found in ranks.yml!");
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection rankSection = section.getConfigurationSection(key);
            if (rankSection != null) {
                Rank rank = new Rank(key, rankSection);

                // Apply prestige cost multiplier if applicable
                // (handled dynamically by RankUpManager based on prestige level)
                ranks.put(key.toUpperCase(), rank);
                rankOrder.add(key.toUpperCase());
            }
        }

        plugin.getLogger().info("Loaded " + ranks.size() + " ranks.");
    }

    public void reload() {
        load();
    }

    public Rank getRank(String id) {
        return ranks.get(id.toUpperCase());
    }

    public boolean rankExists(String id) {
        return ranks.containsKey(id.toUpperCase());
    }

    /** Returns the rank after the given rank in the ladder, or null if max. */
    public Rank getNextRank(String currentRankId) {
        int idx = rankOrder.indexOf(currentRankId.toUpperCase());
        if (idx < 0 || idx >= rankOrder.size() - 1) return null;
        return ranks.get(rankOrder.get(idx + 1));
    }

    /** Returns the first rank in the ladder (the starting rank). */
    public Rank getFirstRank() {
        if (rankOrder.isEmpty()) return null;
        return ranks.get(rankOrder.get(0));
    }

    /** Returns the last rank in the ladder. */
    public Rank getLastRank() {
        if (rankOrder.isEmpty()) return null;
        return ranks.get(rankOrder.get(rankOrder.size() - 1));
    }

    public boolean isMaxRank(String rankId) {
        return rankOrder.indexOf(rankId.toUpperCase()) == rankOrder.size() - 1;
    }

    public int getRankIndex(String rankId) {
        return rankOrder.indexOf(rankId.toUpperCase());
    }

    public int getTotalRanks() {
        return rankOrder.size();
    }

    public Collection<Rank> getAllRanks() {
        return ranks.values();
    }

    public List<String> getRankOrder() {
        return Collections.unmodifiableList(rankOrder);
    }

    /**
     * Persists a single edited field back to ranks.yml (used by the admin rank
     * editor GUI) and saves the file. The in-memory Rank object should already
     * have been updated by the caller via its setter before calling this.
     */
    public void persistRankField(String rankId, String key, Object value) {
        plugin.getConfigManager().getRanksConfig().set("ranks." + rankId + "." + key, value);
        plugin.getConfigManager().saveRanksFile();
    }

    /**
     * Gets the effective cost for a rank, factoring in prestige multiplier.
     */
    public double getEffectiveCost(Rank rank, int prestige) {
        if (prestige <= 0) return rank.getCost();
        double multiplier = Math.pow(plugin.getConfigManager().getPrestigeCostMultiplier(), prestige);
        return rank.getCost() * multiplier;
    }
}
