package com.kazumiii.rankupplus.models;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Rank {

    private final String id;
    private final String display;
    private String color;
    private final String permission;
    private final List<String> lore;
    private double cost;
    private final List<RankRequirement> requirements;
    private final List<String> commands;
    private final List<String> playerCommands;
    private final List<RankReward> rewards;
    private boolean prestige;

    public Rank(String id, ConfigurationSection section) {
        this.id = id;
        this.display = section.getString("display", id);
        this.color = section.getString("color", "f");
        this.permission = section.getString("permission", "rank-" + id.toLowerCase());
        this.lore = section.getStringList("lore");
        this.cost = section.getDouble("cost", 0);
        this.prestige = section.getBoolean("prestige", false);

        this.requirements = new ArrayList<>();
        if (section.isList("requirements")) {
            for (Object obj : section.getList("requirements")) {
                if (obj instanceof java.util.LinkedHashMap<?, ?> map) {
                    String type = (String) map.get("type");
                    Object rawValue = map.get("value");
                    double value = rawValue instanceof Number ? ((Number) rawValue).doubleValue() : 0;
                    Object displayRaw = map.get("display");
                    String reqDisplay = displayRaw != null ? (String) displayRaw : type;
                    String permissionNode = (String) map.get("permission");
                    String statisticName = (String) map.get("statistic");
                    requirements.add(new RankRequirement(type, value, reqDisplay, permissionNode, statisticName));
                }
            }
        }

        this.commands = section.getStringList("commands");
        this.playerCommands = section.getStringList("player-commands");

        this.rewards = new ArrayList<>();
        List<?> rewardList = section.getList("rewards");
        if (rewardList != null) {
            for (Object obj : rewardList) {
                if (obj instanceof java.util.LinkedHashMap<?, ?> map) {
                    rewards.add(RankReward.fromMap(map));
                }
            }
        }
    }

    public String getId() { return id; }
    public String getDisplay() { return display; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getPermission() { return permission; }
    public List<String> getLore() { return lore; }
    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
    public List<RankRequirement> getRequirements() { return requirements; }
    public List<String> getCommands() { return commands; }
    public List<String> getPlayerCommands() { return playerCommands; }
    public List<RankReward> getRewards() { return rewards; }
    public boolean isPrestige() { return prestige; }
    public void setPrestige(boolean prestige) { this.prestige = prestige; }

    public String getColoredDisplay() {
        return ChatColor.getByChar(color) + display;
    }

    public String getDisplayName() {
        return ChatColor.getByChar(color) + "" + ChatColor.BOLD + display;
    }
}
