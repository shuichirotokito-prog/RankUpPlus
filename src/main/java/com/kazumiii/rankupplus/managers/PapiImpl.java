package com.kazumiii.rankupplus.managers;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/**
 * Holds the one direct reference to me.clip.placeholderapi.PlaceholderAPI.
 * Deliberately kept separate from PapiBridge — see the comment on that class.
 *
 * Package-private: only PapiBridge should ever touch this class.
 */
class PapiImpl {

    String apply(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
