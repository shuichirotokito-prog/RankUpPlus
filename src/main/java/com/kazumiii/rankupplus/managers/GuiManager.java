package com.kazumiii.rankupplus.managers;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.models.PlayerData;
import com.kazumiii.rankupplus.models.Rank;
import com.kazumiii.rankupplus.models.RankRequirement;
import com.kazumiii.rankupplus.utils.ColorUtils;
import com.kazumiii.rankupplus.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GuiManager {

    private final RankUpPlus plugin;
    private ItemStack fillerItem;
    private ItemStack accentItem;

    /**
     * RankUpPlus's signature divider: a short strikethrough line, a centered
     * ✦, then another short strikethrough line. This unifies two elements
     * that already existed separately elsewhere in the plugin — the ✦ motif
     * used in every GUI title, and the &m-strikethrough divider technique
     * already used in chat command output (/rankadmin, /ranks, /ranktop) —
     * into one consistent signature element, used here in GUI lore too.
     * Deliberately not the generic repeated-bar-character convention
     * (e.g. "▬▬▬▬▬▬▬▬") common across many other inventory-GUI plugins.
     */
    private static final String DIVIDER = "&8&m      &8✦&8&m      ";

    public GuiManager(RankUpPlus plugin) {
        this.plugin = plugin;
        buildFiller();
    }

    public void reload() {
        buildFiller();
    }

    private void buildFiller() {
        Material mat;
        try {
            mat = Material.valueOf(plugin.getConfigManager().getFillerMaterial());
        } catch (IllegalArgumentException e) {
            mat = Material.BLACK_STAINED_GLASS_PANE;
        }
        fillerItem = new ItemBuilder(mat).name(" ").build();
        accentItem = new ItemBuilder(Material.CYAN_STAINED_GLASS_PANE).name(" ").build();
    }

    // =============================================
    //   MAIN RANKUP GUI
    // =============================================
    public Inventory buildRankUpGui(Player player) {
        String title = ColorUtils.color(plugin.getConfig().getString("gui.rankup-menu.title", "&8✦ &bRank Up &8✦"));
        int rows = Math.max(6, plugin.getConfig().getInt("gui.rankup-menu.rows", 6));
        Inventory inv = Bukkit.createInventory(null, rows * 9, title);

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return inv;

        String currentRankId = data.getCurrentRankId();
        Rank currentRank = plugin.getRankManager().getRank(currentRankId);
        Rank nextRank = plugin.getRankManager().getNextRank(currentRankId);

        // Fill background
        fillBackground(inv, rows);

        // Progress bar row (row 2, slots 9-17)
        buildProgressRow(inv, data);

        // Current rank display (slot 20)
        if (currentRank != null) {
            inv.setItem(20, buildCurrentRankItem(player, data, currentRank));
        }

        // Arrow (slot 22)
        inv.setItem(22, new ItemBuilder(Material.ARROW)
            .name("&b➜ &3Next Rank")
            .lore("&7Click to rank up!")
            .build());

        // Next rank display (slot 24)
        if (nextRank != null) {
            inv.setItem(24, buildNextRankItem(player, data, nextRank));
        } else if (plugin.getPrestigeManager().canPrestige(player)) {
            inv.setItem(24, buildPrestigeItem(player, data));
        } else {
            inv.setItem(24, new ItemBuilder(Material.BARRIER)
                .name("&c✖ Max Rank")
                .lore("&7You have reached the maximum rank!")
                .build());
        }

        // Rank-up button (slot 40)
        inv.setItem(40, buildRankUpButton(player, data, nextRank));

        // Stats item (slot 48)
        inv.setItem(48, buildStatsItem(player, data));

        // All ranks button (slot 50)
        inv.setItem(50, new ItemBuilder(Material.BOOK)
            .name("&b📖 &3All Ranks")
            .lore("&7Click to view all ranks!")
            .build());

        // Close button (slot 49)
        inv.setItem(49, new ItemBuilder(Material.BARRIER)
            .name("&c✖ Close")
            .lore("&7Click to close this menu.")
            .build());

        return inv;
    }

    // =============================================
    //   ALL RANKS OVERVIEW GUI
    // =============================================
    public Inventory buildRanksOverview(Player player) {
        String title = ColorUtils.color(plugin.getConfig().getString("gui.ranks-overview.title", "&8✦ &bAll Ranks &8✦"));
        int rows = Math.max(6, plugin.getConfig().getInt("gui.ranks-overview.rows", 6));
        Inventory inv = Bukkit.createInventory(null, rows * 9, title);

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return inv;

        fillBackground(inv, rows);

        List<Rank> allRanks = new ArrayList<>(plugin.getRankManager().getAllRanks());
        int currentIndex = plugin.getRankManager().getRankIndex(data.getCurrentRankId());
        int slot = 10;
        int count = 0;
        // 7 usable columns per row, reserving the top row (search/header) and
        // bottom row (back button) as borders.
        int maxItems = Math.max(0, (rows - 2)) * 7;

        for (int i = 0; i < allRanks.size() && count < maxItems; i++) {
            Rank rank = allRanks.get(i);
            int rankIndex = i;

            ItemBuilder builder;
            if (rankIndex < currentIndex) {
                // Completed rank
                builder = new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                    .name(rank.getColoredDisplay() + " &8[&a✔ Completed&8]");
            } else if (rankIndex == currentIndex) {
                // Current rank
                builder = new ItemBuilder(Material.GOLD_BLOCK)
                    .name(rank.getColoredDisplay() + " &8[&eCurrent&8]")
                    .glow(true);
            } else {
                // Future rank
                builder = buildRankOverviewItem(player, data, rank, rankIndex == currentIndex + 1);
            }

            // Add lore
            List<String> lore = new ArrayList<>();
            lore.add(DIVIDER);
            for (String l : rank.getLore()) lore.add(ColorUtils.color(l));
            double cost = plugin.getRankManager().getEffectiveCost(rank, data.getPrestige());
            lore.add("&7Cost: &e" + MessageManager.formatMoney(cost));

            if (!rank.getRequirements().isEmpty()) {
                lore.add(DIVIDER);
                lore.add("&7Requirements:");
                for (RankRequirement req : rank.getRequirements()) {
                    String reqStr = plugin.getRequirementManager()
                        .getRequirementProgress(player, data, req);
                    lore.add("  " + ColorUtils.color(reqStr));
                }
            }

            builder.lore(lore);
            inv.setItem(slot, builder.build());
            count++;
            slot++;
            if ((slot + 1) % 9 == 0) slot += 2; // Skip edges
        }

        // Back button
        inv.setItem(49, new ItemBuilder(Material.ARROW)
            .name("&c← Back")
            .lore("&7Return to Rank Up menu.")
            .build());

        return inv;
    }

    // =============================================
    //   CONFIRM RANKUP GUI
    // =============================================
    public Inventory buildConfirmGui(Player player, Rank toRank) {
        String title = ColorUtils.color(plugin.getConfig().getString("gui.confirm-menu.title", "&8Confirm Rankup"));
        int rows = Math.max(3, plugin.getConfig().getInt("gui.confirm-menu.rows", 3));
        Inventory inv = Bukkit.createInventory(null, rows * 9, title);

        fillBackground(inv, rows);

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        Rank playerCurrentRank = data != null ? plugin.getRankManager().getRank(data.getCurrentRankId()) : null;
        double cost = (data != null && playerCurrentRank != null)
            ? plugin.getRankManager().getEffectiveCost(playerCurrentRank, data.getPrestige())
            : 0;

        // Info item (center)
        inv.setItem(13, new ItemBuilder(Material.ENDER_EYE)
            .name("&b⚡ Confirm Rank Up")
            .lore(
                "&7Rank: &r" + toRank.getColoredDisplay(),
                "&7Cost: &e" + MessageManager.formatMoney(cost),
                "",
                "&aClick &2✔ Confirm &ato proceed.",
                "&cClick &4✖ Cancel &cto go back."
            )
            .glow(true)
            .build());

        // Confirm (slot 11)
        inv.setItem(11, new ItemBuilder(Material.LIME_CONCRETE)
            .name("&a✔ Confirm")
            .lore("&7Rank up to " + toRank.getColoredDisplay())
            .build());

        // Cancel (slot 15)
        inv.setItem(15, new ItemBuilder(Material.RED_CONCRETE)
            .name("&c✖ Cancel")
            .lore("&7Return to rank up menu.")
            .build());

        return inv;
    }

    // =============================================
    //   ADMIN RANK EDITOR GUI
    // =============================================
    private static final String[] COLOR_PALETTE = {
        "0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"
    };

    private ItemStack buildCostDisplayItem(Rank rank) {
        return new ItemBuilder(Material.GOLD_INGOT)
            .name("&e&lCost: &f" + MessageManager.formatMoney(rank.getCost()))
            .lore(
                "&7Click the &c-/&a+ &7buttons either side",
                "&7to adjust this rank's base cost.",
                "&8(Prestige multiplier is applied on top of this.)"
            )
            .build();
    }

    private ItemStack buildColorDisplayItem(Rank rank) {
        return new ItemBuilder(Material.WHITE_DYE)
            .name("&e&lColor Code: &" + rank.getColor() + rank.getDisplay())
            .lore(
                "&7Click to cycle to the next color.",
                "&7Current code: &f&" + rank.getColor()
            )
            .build();
    }

    private ItemStack buildPrestigeToggleItem(Rank rank) {
        return new ItemBuilder(rank.isPrestige() ? Material.LIME_DYE : Material.GRAY_DYE)
            .name("&e&lPrestige Rank: " + (rank.isPrestige() ? "&aON" : "&cOFF"))
            .lore(
                "&7Whether reaching this rank",
                "&7triggers a prestige reset.",
                "&7Click to toggle."
            )
            .build();
    }

    private ItemStack buildRankInfoItem(Rank rank) {
        return new ItemBuilder(Material.BOOK)
            .name("&b&lOther Settings &7(read-only)")
            .lore(
                "&7Display: &f" + rank.getDisplay(),
                "&7Permission group: &f" + rank.getPermission(),
                "&7Rewards: &f" + rank.getRewards().size(),
                "&7Console commands: &f" + rank.getCommands().size(),
                "&7Player commands: &f" + rank.getPlayerCommands().size(),
                "",
                "&8Edit display name, permission, lore,",
                "&8rewards, and commands directly in",
                "&8ranks.yml then /rankadmin reload."
            )
            .build();
    }

    private ItemStack buildRequirementsButton(Rank rank) {
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("&7Current requirements:");
        if (rank.getRequirements().isEmpty()) {
            lore.add("  &8None");
        } else {
            for (com.kazumiii.rankupplus.models.RankRequirement req : rank.getRequirements()) {
                lore.add("  &7- &f" + req.getTypeRaw() + " &8(" + (int) req.getValue() + ")");
            }
        }
        lore.add("");
        lore.add("&eClick to open the requirements editor.");
        return new ItemBuilder(Material.PAPER)
            .name("&b&lRequirements &7(" + rank.getRequirements().size() + ")")
            .lore(lore.toArray(new String[0]))
            .build();
    }

    public Inventory buildRankEditorGui(Rank rank) {
        String title = ColorUtils.color("&8✦ &bEdit Rank&8: &f" + rank.getId() + " &8✦");
        Inventory inv = Bukkit.createInventory(null, 36, title);
        fillBackground(inv, 4);

        // Row 1: cost adjustment
        inv.setItem(11, new ItemBuilder(Material.RED_CONCRETE).name("&c-1000").build());
        inv.setItem(12, new ItemBuilder(Material.RED_CONCRETE).name("&c-100").build());
        inv.setItem(13, buildCostDisplayItem(rank));
        inv.setItem(14, new ItemBuilder(Material.LIME_CONCRETE).name("&a+100").build());
        inv.setItem(15, new ItemBuilder(Material.LIME_CONCRETE).name("&a+1000").build());

        // Row 2: color / prestige / requirements / info
        inv.setItem(20, buildColorDisplayItem(rank));
        inv.setItem(21, buildPrestigeToggleItem(rank));
        inv.setItem(22, buildRequirementsButton(rank));
        inv.setItem(23, buildRankInfoItem(rank));

        inv.setItem(31, new ItemBuilder(Material.BARRIER).name("&c✖ Close").build());

        return inv;
    }

    // ---- Requirements Editor GUI ----

    /**
     * Builds a requirements editor inventory: one item per existing requirement
     * with a left-click-to-remove action, plus a green glass "Add new" button.
     * Allows admins to add and remove requirements for a rank in-game without
     * touching ranks.yml directly.
     */
    public Inventory buildRequirementsEditorGui(Rank rank) {
        String title = ColorUtils.color("&8Requirements: &b" + rank.getId());
        int size = 54;
        Inventory inv = Bukkit.createInventory(null, size, title);
        fillBackground(inv, 6);

        java.util.List<com.kazumiii.rankupplus.models.RankRequirement> reqs = rank.getRequirements();
        for (int i = 0; i < Math.min(reqs.size(), 44); i++) {
            com.kazumiii.rankupplus.models.RankRequirement req = reqs.get(i);
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("&7Type: &f" + req.getTypeRaw());
            lore.add("&7Value: &f" + (int) req.getValue());
            lore.add("&7Display: &f" + (req.getDisplay() != null ? req.getDisplay() : "&8none"));
            if (req.getPermission() != null) lore.add("&7Permission: &f" + req.getPermission());
            if (req.getStatisticName() != null) lore.add("&7Statistic: &f" + req.getStatisticName());
            lore.add("");
            lore.add("&cLeft-click to remove this requirement.");
            inv.setItem(i, new ItemBuilder(Material.PAPER)
                .name("&f" + req.getTypeRaw() + " &8» &e" + (int) req.getValue())
                .lore(lore.toArray(new String[0]))
                .build());
        }

        // Add quick-add buttons for the most common requirement types
        inv.setItem(45, buildAddReqButton("PLAYTIME",   100, "&7100 minutes playtime",      Material.CLOCK));
        inv.setItem(46, buildAddReqButton("KILLS",      50,  "&c50 player kills",           Material.IRON_SWORD));
        inv.setItem(47, buildAddReqButton("LEVEL",      30,  "&a30 XP levels",              Material.EXPERIENCE_BOTTLE));
        inv.setItem(48, buildAddReqButton("BALANCE",    50000, "&e$50,000 balance",          Material.GOLD_INGOT));
        inv.setItem(49, buildAddReqButton("BLOCKS_BROKEN", 1000, "&71000 blocks broken",    Material.DIAMOND_PICKAXE));
        inv.setItem(50, buildAddReqButton("DEATHS",     25,  "&c25 deaths",                 Material.SKELETON_SKULL));
        inv.setItem(51, buildAddReqButton("BLOCKS_PLACED", 1000, "&71000 blocks placed",    Material.BRICKS));
        inv.setItem(52, new ItemBuilder(Material.ARROW).name("&7← Back to rank editor").build());
        inv.setItem(53, new ItemBuilder(Material.BARRIER).name("&c✖ Close").build());

        return inv;
    }

    private ItemStack buildAddReqButton(String type, double defaultValue, String desc, Material mat) {
        return new ItemBuilder(mat)
            .name("&a+ Add &f" + type)
            .lore(
                "&7Adds a " + type + " requirement",
                "&7with value &e" + (int) defaultValue,
                "&8(" + desc + ")",
                "",
                "&aLeft-click to add with default value.",
                "&eShift-click opens the value picker."
            )
            .build();
    }

    /** Saves the current in-memory requirements list for a rank back to ranks.yml. */
    public void persistRequirements(Rank rank) {
        java.util.List<java.util.Map<String, Object>> reqList = new java.util.ArrayList<>();
        for (com.kazumiii.rankupplus.models.RankRequirement req : rank.getRequirements()) {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("type", req.getTypeRaw());
            map.put("value", (int) req.getValue());
            if (req.getDisplay() != null) map.put("display", req.getDisplay());
            if (req.getPermission() != null) map.put("permission", req.getPermission());
            if (req.getStatisticName() != null) map.put("statistic", req.getStatisticName());
            reqList.add(map);
        }
        plugin.getConfigManager().getRanksConfig().set("ranks." + rank.getId() + ".requirements", reqList);
        plugin.getConfigManager().saveRanksFile();
    }

    public void refreshRequirementsButton(Inventory inv, Rank rank) {
        inv.setItem(22, buildRequirementsButton(rank));
    }

    /** Re-renders just the cost display item after an edit, without reopening the whole GUI. */
    public void refreshRankEditorCost(Inventory inv, Rank rank) {
        inv.setItem(13, buildCostDisplayItem(rank));
    }

    public void refreshRankEditorColor(Inventory inv, Rank rank) {
        inv.setItem(20, buildColorDisplayItem(rank));
    }

    public void refreshRankEditorPrestige(Inventory inv, Rank rank) {
        inv.setItem(21, buildPrestigeToggleItem(rank));
    }

    public static String nextColor(String current) {
        int idx = -1;
        for (int i = 0; i < COLOR_PALETTE.length; i++) {
            if (COLOR_PALETTE[i].equalsIgnoreCase(current)) { idx = i; break; }
        }
        return COLOR_PALETTE[(idx + 1) % COLOR_PALETTE.length];
    }

    // =============================================
    //   HELPERS
    // =============================================

    private void fillBackground(Inventory inv, int rows) {
        for (int i = 0; i < rows * 9; i++) {
            inv.setItem(i, fillerItem.clone());
        }
        // Signature corner accent — a small, consistent brand touch on every
        // GUI rather than a flat, undifferentiated border.
        inv.setItem(0, accentItem.clone());
        inv.setItem(8, accentItem.clone());
        inv.setItem((rows - 1) * 9, accentItem.clone());
        inv.setItem((rows - 1) * 9 + 8, accentItem.clone());
    }

    private void buildProgressRow(Inventory inv, PlayerData data) {
        int totalRanks = plugin.getRankManager().getTotalRanks();
        int currentIndex = plugin.getRankManager().getRankIndex(data.getCurrentRankId());
        double progress = totalRanks <= 1 ? 1.0 : (double) currentIndex / (totalRanks - 1);

        int barLength = 7;
        int filled = (int) Math.round(progress * barLength);

        for (int i = 0; i < barLength; i++) {
            Material mat = i < filled ? Material.CYAN_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE;
            String name = i < filled ? "&b&l▌" : "&8&l▌";
            inv.setItem(10 + i, new ItemBuilder(mat).name(name).build());
        }
    }

    private ItemStack buildCurrentRankItem(Player player, PlayerData data, Rank rank) {
        double balance = plugin.getEconomyManager().getBalance(player);
        double cost = plugin.getRankManager().getEffectiveCost(rank, data.getPrestige());
        return new ItemBuilder(Material.PLAYER_HEAD)
            .name("&b⭐ Current Rank: &r" + rank.getColoredDisplay())
            .playerHead(player)
            .lore(
                DIVIDER,
                "&7Prestige: &5" + (data.getPrestige() > 0 ? "[P" + data.getPrestige() + "]" : "None"),
                "&7Balance: &e" + MessageManager.formatMoney(balance),
                "&7Kills: &c" + data.getPlayerKills(),
                "&7Playtime: &b" + data.getTotalPlaytime() + "m",
                DIVIDER,
                "&7Next rankup cost: &e" + MessageManager.formatMoney(cost)
            )
            .build();
    }

    private ItemStack buildNextRankItem(Player player, PlayerData data, Rank nextRank) {
        List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        for (String l : nextRank.getLore()) lore.add(ColorUtils.color(l));
        lore.add(DIVIDER);

        Rank currentRank = plugin.getRankManager().getRank(data.getCurrentRankId());
        double cost = currentRank != null
            ? plugin.getRankManager().getEffectiveCost(currentRank, data.getPrestige()) : 0;
        lore.add("&7Cost: &e" + MessageManager.formatMoney(cost));

        if (!nextRank.getRequirements().isEmpty()) {
            lore.add("&7Requirements:");
            for (RankRequirement req : nextRank.getRequirements()) {
                lore.add("  " + ColorUtils.color(
                    plugin.getRequirementManager().getRequirementProgress(player, data, req)));
            }
        }

        if (!nextRank.getRewards().isEmpty()) {
            lore.add("&7Rewards: &a" + nextRank.getRewards().size() + " item(s)");
        }

        return new ItemBuilder(Material.DIAMOND)
            .name("&b✦ Next Rank: &r" + nextRank.getColoredDisplay())
            .lore(lore)
            .glow(true)
            .build();
    }

    private ItemStack buildPrestigeItem(Player player, PlayerData data) {
        return new ItemBuilder(Material.BEACON)
            .name("&5✦ PRESTIGE AVAILABLE! ✦")
            .lore(
                "&7You have reached the maximum rank!",
                "&5Click Rank Up to Prestige.",
                "&7Current Prestige: &5" + data.getPrestige(),
                "&7Next Prestige: &5" + (data.getPrestige() + 1)
            )
            .glow(true)
            .build();
    }

    private ItemStack buildRankUpButton(Player player, PlayerData data, Rank nextRank) {
        if (nextRank == null && !plugin.getPrestigeManager().canPrestige(player)) {
            return new ItemBuilder(Material.BARRIER)
                .name("&c✖ Maximum Rank")
                .lore("&7You cannot rank up further!")
                .build();
        }

        Rank currentRank = plugin.getRankManager().getRank(data.getCurrentRankId());
        double cost = currentRank != null
            ? plugin.getRankManager().getEffectiveCost(currentRank, data.getPrestige()) : 0;
        double balance = plugin.getEconomyManager().getBalance(player);
        boolean canAfford = balance >= cost;

        List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        lore.add("&7Cost: " + (canAfford ? "&a" : "&c") + MessageManager.formatMoney(cost));
        lore.add("&7Balance: &e" + MessageManager.formatMoney(balance));

        if (!canAfford) {
            lore.add("&c✖ &cNot enough money!");
        } else {
            lore.add("&a✔ &aYou can rank up!");
        }

        return new ItemBuilder(canAfford ? Material.LIME_CONCRETE : Material.RED_CONCRETE)
            .name(canAfford ? "&a⬆ &2RANK UP!" : "&c✖ Can't Rank Up")
            .lore(lore)
            .glow(canAfford)
            .build();
    }

    private ItemStack buildStatsItem(Player player, PlayerData data) {
        return new ItemBuilder(Material.PAPER)
            .name("&b📊 &3Your Stats")
            .lore(
                DIVIDER,
                "&7Total Playtime: &b" + data.getTotalPlaytime() + "m",
                "&7Player Kills: &c" + data.getPlayerKills(),
                "&7Deaths: &e" + data.getDeaths(),
                "&7Prestige: &5" + (data.getPrestige() > 0 ? data.getPrestige() : "None"),
                DIVIDER
            )
            .build();
    }

    private ItemBuilder buildRankOverviewItem(Player player, PlayerData data, Rank rank, boolean isNext) {
        Material mat = isNext ? Material.GOLD_INGOT : Material.IRON_INGOT;
        return new ItemBuilder(mat)
            .name(rank.getColoredDisplay() + (isNext ? " &e← Next" : ""))
            .glow(isNext);
    }
}
