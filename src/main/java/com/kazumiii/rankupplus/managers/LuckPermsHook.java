package com.kazumiii.rankupplus.managers;

import com.kazumiii.rankupplus.RankUpPlus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Thin outer wrapper around LuckPerms group sync. This class itself references
 * NO LuckPerms types in any field, method signature, or constructor — that's
 * deliberate. The JVM resolves every type in a class's signatures the moment
 * the class is loaded, regardless of whether the code path that uses those
 * types actually runs. Since LuckPerms is a soft/optional dependency
 * (compileOnly, not bundled in our jar), referencing its types directly here
 * would throw NoClassDefFoundError at plugin startup on any server that
 * doesn't have LuckPerms installed — even though we only ever call into it
 * after confirming it's present.
 *
 * The actual API calls live in LuckPermsImpl, a separate class that is only
 * ever loaded (via the JVM's lazy class-loading) once isAvailable() has
 * already confirmed LuckPerms is on the server. Until then, LuckPermsImpl's
 * bytecode is never touched, so its references to net.luckperms.api.* types
 * never get resolved and can't crash anything.
 */
public class LuckPermsHook {

    private final RankUpPlus plugin;
    private LuckPermsImpl impl;
    private boolean checked = false;
    private boolean available = false;

    public LuckPermsHook(RankUpPlus plugin) {
        this.plugin = plugin;
    }

    public boolean isAvailable() {
        if (!plugin.getConfigManager().isLuckPermsIntegration()) return false;
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) return false;

        if (!checked) {
            checked = true;
            try {
                impl = new LuckPermsImpl();
                available = impl.tryInit();
            } catch (Throwable t) {
                // Covers NoClassDefFoundError / LinkageError if LuckPerms is present
                // but an unexpectedly incompatible version, plus any other surprise.
                plugin.getLogger().warning("LuckPerms was detected but its API could not be hooked: " + t.getMessage());
                impl = null;
                available = false;
            }
        }
        return available;
    }

    public void setGroup(Player player, String oldGroup, String newGroup) {
        if (!isAvailable()) return;
        impl.setGroup(player.getUniqueId(), oldGroup, newGroup);
    }

    public void setGroup(UUID uuid, String oldGroup, String newGroup) {
        if (!isAvailable()) return;
        impl.setGroup(uuid, oldGroup, newGroup);
    }

    public void addGroupIfMissing(Player player, String group) {
        if (!isAvailable()) return;
        if (group == null || group.isBlank()) return;
        if (player.hasPermission("group." + group)) return;
        impl.setGroup(player.getUniqueId(), null, group);
    }
}
