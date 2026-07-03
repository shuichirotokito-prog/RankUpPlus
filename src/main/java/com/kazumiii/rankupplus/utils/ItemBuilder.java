package com.kazumiii.rankupplus.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder name(String name) {
        if (meta != null) meta.setDisplayName(ColorUtils.color(name));
        return this;
    }

    public ItemBuilder lore(String... lines) {
        if (meta == null) return this;
        List<String> coloredLore = new ArrayList<>();
        for (String line : lines) coloredLore.add(ColorUtils.color(line));
        meta.setLore(coloredLore);
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        if (meta == null) return this;
        List<String> coloredLore = new ArrayList<>();
        for (String line : lines) coloredLore.add(ColorUtils.color(line));
        meta.setLore(coloredLore);
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        if (meta == null || !glow) return this;
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    public ItemBuilder hideFlags() {
        if (meta != null) meta.addItemFlags(ItemFlag.values());
        return this;
    }

    public ItemBuilder customModelData(int data) {
        if (meta != null) meta.setCustomModelData(data);
        return this;
    }

    public ItemBuilder playerHead(Player player) {
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) item.setItemMeta(meta);
        return item;
    }
}
