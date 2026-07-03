package com.kazumiii.rankupplus.utils;

import org.bukkit.ChatColor;

public class ColorUtils {

    public static String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String strip(String text) {
        return ChatColor.stripColor(color(text));
    }

    /**
     * Removes any literal section-sign (§, code point 0x00A7) characters from
     * text, without translating '&' codes. Use this on player-supplied text
     * (like a chat message) before embedding it into an already-colorized
     * string: the Minecraft client renders a raw § as a color code regardless
     * of what server-side &-translation logic does or doesn't run, so a
     * player pasting or typing a literal § needs to be neutralized separately
     * from the normal &-translation path.
     */
    public static String stripLiteralSectionSigns(String text) {
        if (text == null) return "";
        return text.replace('\u00A7', ' ');
    }

    public static String buildProgressBar(int filled, int total, String filledChar, String emptyChar,
                                          String filledColor, String emptyColor) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < total; i++) {
            if (i < filled) {
                sb.append(color(filledColor)).append(filledChar);
            } else {
                sb.append(color(emptyColor)).append(emptyChar);
            }
        }
        return sb.toString();
    }
}
