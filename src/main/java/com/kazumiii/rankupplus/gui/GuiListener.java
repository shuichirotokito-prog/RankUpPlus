package com.kazumiii.rankupplus.gui;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.managers.RankUpManager;
import com.kazumiii.rankupplus.models.PlayerData;
import com.kazumiii.rankupplus.models.Rank;
import com.kazumiii.rankupplus.utils.ColorUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiListener implements Listener {

    private final RankUpPlus plugin;

    public GuiListener(RankUpPlus plugin) {
        this.plugin = plugin;
    }

    private RankUpManager rankUpManager() {
        return plugin.getRankUpManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = ColorUtils.strip(event.getView().getTitle());
        boolean isOurGui = title.contains("Rank Up") || title.contains("RankUp")
            || title.contains("Confirm Rankup") || title.contains("Confirm RankUp")
            || title.contains("All Ranks") || title.contains("Edit Rank")
            || title.contains("Requirements:");
        if (!isOurGui) return;

        // Cancel ALL clicks while one of our menus is open (including clicks in the
        // player's own inventory) so nothing can be taken/moved, but only ever treat
        // it as a button press if the click happened in the top (GUI) inventory.
        event.setCancelled(true);

        int topSize = event.getView().getTopInventory().getSize();
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= topSize) return; // Click was in the player's own inventory.
        if (event.getCurrentItem() == null) return;

        if (title.contains("Requirements:")) {
            if (!player.hasPermission("rankupplus.admin")) { player.closeInventory(); return; }
            handleRequirementsEditorGui(player, rawSlot, title, event.getView().getTopInventory());
        } else if (title.contains("Edit Rank")) {
            if (!player.hasPermission("rankupplus.admin")) { player.closeInventory(); return; }
            handleRankEditorGui(player, rawSlot, title, event.getView().getTopInventory());
        } else if (title.contains("Confirm Rankup") || title.contains("Confirm RankUp")) {
            handleConfirmGui(player, rawSlot);
        } else if (title.contains("All Ranks")) {
            handleRanksOverview(player, rawSlot);
        } else if (title.contains("Rank Up") || title.contains("RankUp")) {
            handleRankUpGui(player, rawSlot);
        }
    }

    private void handleRankUpGui(Player player, int slot) {
        switch (slot) {
            case 40 -> { // Rank Up button
                player.closeInventory();
                doRankUp(player);
            }
            case 49 -> player.closeInventory(); // Close
            case 50 -> { // All ranks
                player.openInventory(plugin.getGuiManager().buildRanksOverview(player));
            }
        }
    }

    private void handleConfirmGui(Player player, int slot) {
        if (slot == 11) { // Confirm
            player.closeInventory();
            doRankUp(player);
        } else if (slot == 15) { // Cancel
            player.closeInventory();
            plugin.getMessageManager().send(player, "rankup-cancelled");
        }
    }

    private void handleRanksOverview(Player player, int slot) {
        if (slot == 49) { // Back button
            player.openInventory(plugin.getGuiManager().buildRankUpGui(player));
        }
    }

    private void handleRankEditorGui(Player player, int slot, String strippedTitle, org.bukkit.inventory.Inventory inv) {
        String rankId = parseRankIdFromTitle(strippedTitle);
        if (rankId == null) return;
        Rank rank = plugin.getRankManager().getRank(rankId);
        if (rank == null) return;

        switch (slot) {
            case 11 -> adjustCost(rank, inv, -1000);
            case 12 -> adjustCost(rank, inv, -100);
            case 14 -> adjustCost(rank, inv, 100);
            case 15 -> adjustCost(rank, inv, 1000);
            case 20 -> {
                String next = com.kazumiii.rankupplus.managers.GuiManager.nextColor(rank.getColor());
                rank.setColor(next);
                plugin.getRankManager().persistRankField(rank.getId(), "color", next);
                plugin.getGuiManager().refreshRankEditorColor(inv, rank);
                plugin.getScoreboardManager().refreshAll();
            }
            case 21 -> {
                boolean next = !rank.isPrestige();
                rank.setPrestige(next);
                plugin.getRankManager().persistRankField(rank.getId(), "prestige", next);
                plugin.getGuiManager().refreshRankEditorPrestige(inv, rank);
            }
            case 22 -> {
                // Open the requirements sub-editor
                player.openInventory(plugin.getGuiManager().buildRequirementsEditorGui(rank));
            }
            case 31 -> player.closeInventory();
        }
    }

    private void handleRequirementsEditorGui(Player player, int slot, String strippedTitle, org.bukkit.inventory.Inventory inv) {
        // Title format: "Requirements: RANKID"
        String rankId = strippedTitle.replace("Requirements:", "").trim();
        Rank rank = plugin.getRankManager().getRank(rankId);
        if (rank == null) return;

        int reqCount = rank.getRequirements().size();

        if (slot == 52) {
            // Back to rank editor
            player.openInventory(plugin.getGuiManager().buildRankEditorGui(rank));
            return;
        }
        if (slot == 53) { player.closeInventory(); return; }

        // Slots 45-51: quick-add buttons
        if (slot >= 45 && slot <= 51) {
            String type = switch (slot) {
                case 45 -> "PLAYTIME";
                case 46 -> "KILLS";
                case 47 -> "LEVEL";
                case 48 -> "BALANCE";
                case 49 -> "BLOCKS_BROKEN";
                case 50 -> "DEATHS";
                case 51 -> "BLOCKS_PLACED";
                default -> null;
            };
            double defVal = switch (slot) {
                case 45 -> 100;
                case 46 -> 50;
                case 47 -> 30;
                case 48 -> 50000;
                case 49 -> 1000;
                case 50 -> 25;
                case 51 -> 1000;
                default -> 1;
            };
            if (type != null) {
                rank.getRequirements().add(new com.kazumiii.rankupplus.models.RankRequirement(type, defVal, null));
                plugin.getGuiManager().persistRequirements(rank);
                // Refresh the requirements editor
                player.openInventory(plugin.getGuiManager().buildRequirementsEditorGui(rank));
                player.sendMessage(ColorUtils.color("&aAdded " + type + " requirement with default value &e" + (int) defVal + "&a. Edit the value in ranks.yml or shift-click the button."));
            }
            return;
        }

        // Slots 0-43: existing requirements — left-click removes.
        // Capped at 44 to match buildRequirementsEditorGui's display limit —
        // without this cap, a rank with more than 44 requirements could have
        // slot 44 (a plain filler pane, never assigned a requirement item)
        // misread as clickable-removable just because reqCount > 44.
        int displayedCount = Math.min(reqCount, 44);
        if (slot < displayedCount) {
            com.kazumiii.rankupplus.models.RankRequirement removed = rank.getRequirements().remove(slot);
            plugin.getGuiManager().persistRequirements(rank);
            player.openInventory(plugin.getGuiManager().buildRequirementsEditorGui(rank));
            player.sendMessage(ColorUtils.color("&cRemoved " + removed.getTypeRaw() + " requirement."));
        }
    }

    private void adjustCost(Rank rank, org.bukkit.inventory.Inventory inv, double delta) {
        double newCost = Math.max(0, rank.getCost() + delta);
        rank.setCost(newCost);
        plugin.getRankManager().persistRankField(rank.getId(), "cost", newCost);
        plugin.getGuiManager().refreshRankEditorCost(inv, rank);
    }

    /** Pulls the rank id back out of the editor GUI's title (set as "...Edit Rank: <ID> ..."). */
    private String parseRankIdFromTitle(String strippedTitle) {
        int idx = strippedTitle.indexOf("Edit Rank:");
        if (idx < 0) return null;
        String after = strippedTitle.substring(idx + "Edit Rank:".length()).trim();
        if (after.isEmpty()) return null;
        return after.split(" ")[0];
    }

    private void doRankUp(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        boolean bypassCost = player.hasPermission("rankupplus.bypass.cost");
        boolean bypassReqs = player.hasPermission("rankupplus.bypass.requirements");

        RankUpManager.RankUpResult result = rankUpManager().attemptRankUp(player, bypassCost, bypassReqs);

        if (result == RankUpManager.RankUpResult.NOT_ENOUGH_MONEY) {
            Rank currentRank = plugin.getRankManager().getRank(data.getCurrentRankId());
            double cost = currentRank != null
                ? plugin.getRankManager().getEffectiveCost(currentRank, data.getPrestige()) : 0;
            double balance = plugin.getEconomyManager().getBalance(player);
            plugin.getMessageManager().send(player, "not-enough-money",
                "{cost}", com.kazumiii.rankupplus.managers.MessageManager.formatMoney(Math.max(0, cost - balance)),
                "{balance}", com.kazumiii.rankupplus.managers.MessageManager.formatMoney(balance));
        } else if (result == RankUpManager.RankUpResult.REQUIREMENTS_NOT_MET) {
            plugin.getMessageManager().send(player, "requirements-not-met");
            Rank nextRank = plugin.getRankManager().getNextRank(data.getCurrentRankId());
            if (nextRank != null) {
                for (String req : plugin.getRequirementManager().getUnmetRequirements(player, nextRank)) {
                    player.sendMessage(ColorUtils.color("  &c✖ " + req));
                }
            }
        } else if (result == RankUpManager.RankUpResult.ALREADY_MAX) {
            plugin.getMessageManager().send(player, "already-max-rank");
        } else if (result == RankUpManager.RankUpResult.ON_COOLDOWN) {
            plugin.getMessageManager().send(player, "on-cooldown",
                "{time}", com.kazumiii.rankupplus.managers.MessageManager.formatTime(
                    rankUpManager().getRemainingCooldown(data)));
        }
    }
}
