package com.kazumiii.rankupplus.utils;

import com.kazumiii.rankupplus.RankUpPlus;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

public class EffectsUtil {

    public static void playRankupEffects(RankUpPlus plugin, org.bukkit.entity.Player player) {
        // Sound
        String soundName = plugin.getConfigManager().getRankupSound();
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound,
                plugin.getConfigManager().getRankupSoundVolume(),
                plugin.getConfigManager().getRankupSoundPitch());
        } catch (IllegalArgumentException ignored) {}

        // Firework
        if (plugin.getConfigManager().isFireworkOnRankup()) {
            launchFirework(player.getLocation(), java.awt.Color.CYAN);
        }
    }

    public static void launchFirework(Location location, java.awt.Color color) {
        Firework fw = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
            .with(FireworkEffect.Type.BALL_LARGE)
            .withColor(Color.fromRGB(color.getRed(), color.getGreen(), color.getBlue()))
            .withFade(Color.WHITE)
            .withTrail()
            .withFlicker()
            .build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
        // Detonate immediately
        fw.detonate();
    }
}
