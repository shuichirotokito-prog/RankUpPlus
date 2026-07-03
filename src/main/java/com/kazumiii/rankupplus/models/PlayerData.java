package com.kazumiii.rankupplus.models;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private String name;
    private String currentRankId;
    private int prestige;
    private long totalPlaytime;   // minutes
    private int playerKills;
    private int deaths;
    private long blocksBroken;
    private long blocksPlaced;
    private long lastRankupTime;
    private long firstJoin;
    private String boosterType;      // "COST" or "REWARDS", or null if no active booster
    private double boosterMultiplier;
    private long boosterExpiresAt;   // epoch millis; 0 or in the past = inactive

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.currentRankId = "DEFAULT";
        this.prestige = 0;
        this.totalPlaytime = 0;
        this.playerKills = 0;
        this.deaths = 0;
        this.blocksBroken = 0;
        this.blocksPlaced = 0;
        this.lastRankupTime = 0;
        this.firstJoin = System.currentTimeMillis();
        this.boosterType = null;
        this.boosterMultiplier = 1.0;
        this.boosterExpiresAt = 0;
    }

    public UUID getUuid() { return uuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCurrentRankId() { return currentRankId; }
    public void setCurrentRankId(String currentRankId) { this.currentRankId = currentRankId; }

    public int getPrestige() { return prestige; }
    public void setPrestige(int prestige) { this.prestige = prestige; }
    public void incrementPrestige() { this.prestige++; }

    public long getTotalPlaytime() { return totalPlaytime; }
    public void setTotalPlaytime(long totalPlaytime) { this.totalPlaytime = totalPlaytime; }
    public void addPlaytime(long minutes) { this.totalPlaytime += minutes; }

    public int getPlayerKills() { return playerKills; }
    public void setPlayerKills(int playerKills) { this.playerKills = playerKills; }
    public void incrementKills() { this.playerKills++; }

    public int getDeaths() { return deaths; }
    public void setDeaths(int deaths) { this.deaths = deaths; }
    public void incrementDeaths() { this.deaths++; }

    public long getBlocksBroken() { return blocksBroken; }
    public void setBlocksBroken(long blocksBroken) { this.blocksBroken = blocksBroken; }
    public void incrementBlocksBroken() { this.blocksBroken++; }

    public long getBlocksPlaced() { return blocksPlaced; }
    public void setBlocksPlaced(long blocksPlaced) { this.blocksPlaced = blocksPlaced; }
    public void incrementBlocksPlaced() { this.blocksPlaced++; }

    public long getLastRankupTime() { return lastRankupTime; }
    public void setLastRankupTime(long lastRankupTime) { this.lastRankupTime = lastRankupTime; }

    public long getFirstJoin() { return firstJoin; }
    public void setFirstJoin(long firstJoin) { this.firstJoin = firstJoin; }

    public String getBoosterType() { return boosterType; }
    public void setBoosterType(String boosterType) { this.boosterType = boosterType; }

    public double getBoosterMultiplier() { return boosterMultiplier; }
    public void setBoosterMultiplier(double boosterMultiplier) { this.boosterMultiplier = boosterMultiplier; }

    public long getBoosterExpiresAt() { return boosterExpiresAt; }
    public void setBoosterExpiresAt(long boosterExpiresAt) { this.boosterExpiresAt = boosterExpiresAt; }

    /** True if a booster of the given type is currently active (set and not expired). */
    public boolean isBoosterActive(String type) {
        return boosterType != null
            && boosterType.equalsIgnoreCase(type)
            && boosterExpiresAt > System.currentTimeMillis();
    }

    public void clearBooster() {
        this.boosterType = null;
        this.boosterMultiplier = 1.0;
        this.boosterExpiresAt = 0;
    }
}
