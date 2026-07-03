package com.kazumiii.rankupplus.managers;

import com.kazumiii.rankupplus.RankUpPlus;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final RankUpPlus plugin;
    private Economy economy;
    private boolean setup = false;

    public EconomyManager(RankUpPlus plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return;
        economy = rsp.getProvider();
        setup = economy != null;
        if (setup) plugin.getLogger().info("Vault economy hooked successfully!");
    }

    public boolean isSetup() { return setup; }

    public double getBalance(Player player) {
        if (!setup) return player.getLevel();
        return economy.getBalance(player);
    }

    public boolean has(Player player, double amount) {
        if (!setup) return player.getLevel() >= amount;
        return economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (!setup) {
            if (player.getLevel() < amount) return false;
            player.setLevel((int)(player.getLevel() - amount));
            return true;
        }
        if (!economy.has(player, amount)) return false;
        economy.withdrawPlayer(player, amount);
        return true;
    }

    public void deposit(Player player, double amount) {
        if (!setup) {
            player.setLevel((int)(player.getLevel() + amount));
            return;
        }
        economy.depositPlayer(player, amount);
    }

    public String formatCurrency(double amount) {
        String formatted = MessageManager.formatCompactNumber(amount);
        if (!setup) return formatted + " XP Levels";
        String singular = economy.currencyNameSingular();
        String plural = economy.currencyNamePlural();
        if (plural != null && !plural.isBlank()) {
            return formatted + " " + plural;
        } else if (singular != null && !singular.isBlank()) {
            return formatted + " " + singular;
        }
        return "$" + formatted;
    }
}
