package com.kazumiii.rankupplus.models;

public class RankRequirement {

    public enum Type {
        PLAYTIME,    // Minutes of playtime
        KILLS,       // Player kills
        DEATHS,      // Deaths
        BALANCE,     // Money (additional to rank cost)
        LEVEL,       // XP level
        BLOCKS_BROKEN,
        BLOCKS_PLACED,
        PERMISSION,  // Has a permission node (see getPermission())
        STATISTIC,   // Any Bukkit statistic (see getStatisticName())
        CUSTOM
    }

    private final String typeRaw;
    private final double value;
    private final String display;
    private final String permission;
    private final String statisticName;

    public RankRequirement(String typeRaw, double value, String display) {
        this(typeRaw, value, display, null, null);
    }

    public RankRequirement(String typeRaw, double value, String display, String permission, String statisticName) {
        this.typeRaw = typeRaw != null ? typeRaw.toUpperCase() : "CUSTOM";
        this.value = value;
        this.display = display;
        this.permission = permission;
        this.statisticName = statisticName;
    }

    public String getTypeRaw() { return typeRaw; }
    public double getValue() { return value; }
    public String getDisplay() { return display; }

    /** The permission node to check, for PERMISSION-type requirements. */
    public String getPermission() { return permission; }

    /** The org.bukkit.Statistic name to check, for STATISTIC-type requirements. */
    public String getStatisticName() { return statisticName; }

    public Type getType() {
        try {
            return Type.valueOf(typeRaw);
        } catch (IllegalArgumentException e) {
            return Type.CUSTOM;
        }
    }
}
