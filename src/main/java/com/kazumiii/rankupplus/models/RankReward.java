package com.kazumiii.rankupplus.models;

import com.kazumiii.rankupplus.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RankReward {

    private final Material material;
    private final int amount;
    private final String name;
    private final List<String> lore;
    private final Map<String, Integer> enchants;

    public RankReward(Material material, int amount, String name, List<String> lore, Map<String, Integer> enchants) {
        this.material = material;
        this.amount = amount;
        this.name = name;
        this.lore = lore;
        this.enchants = enchants;
    }

    @SuppressWarnings("unchecked")
    public static RankReward fromMap(Map<?, ?> rawMap) {
        Map<String, Object> map = (Map<String, Object>) rawMap;

        String matStr = (String) map.getOrDefault("material", "STONE");
        Material material = Material.matchMaterial(matStr);
        if (material == null) material = Material.STONE;

        int amount = ((Number) map.getOrDefault("amount", 1)).intValue();
        String name = (String) map.getOrDefault("name", "&fItem");
        List<String> lore = (List<String>) map.getOrDefault("lore", new ArrayList<String>());
        Map<String, Integer> enchants = (Map<String, Integer>) map.getOrDefault("enchants", new java.util.HashMap<String, Integer>());

        return new RankReward(material, amount, name, lore, enchants);
    }

    public ItemStack toItemStack() {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.color(name));
            List<String> coloredLore = new ArrayList<>();
            for (String l : lore) coloredLore.add(ColorUtils.color(l));
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            Enchantment ench = Enchantment.getByName(entry.getKey());
            if (ench != null) item.addUnsafeEnchantment(ench, entry.getValue());
        }
        return item;
    }

    public Material getMaterial() { return material; }
    public int getAmount() { return amount; }
    public String getName() { return name; }
}
