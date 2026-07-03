package com.kazumiii.rankupplus.managers;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.UUID;

/**
 * Holds every direct reference to net.luckperms.api.* types. Deliberately kept
 * separate from LuckPermsHook — see the comment on that class for why.
 *
 * Package-private: only LuckPermsHook should ever touch this class.
 */
class LuckPermsImpl {

    private LuckPerms api;

    /** Returns true if the LuckPerms API was successfully obtained. */
    boolean tryInit() {
        try {
            api = LuckPermsProvider.get();
            return true;
        } catch (IllegalStateException e) {
            // LuckPerms plugin is present but its API isn't registered yet
            // (e.g. we're being asked about it before LuckPerms finished its own onEnable).
            return false;
        }
    }

    void setGroup(UUID uuid, String oldGroup, String newGroup) {
        if (newGroup == null || newGroup.isBlank()) return;

        User user = api.getUserManager().getUser(uuid);
        if (user != null) {
            applyGroupChange(user, oldGroup, newGroup);
            api.getUserManager().saveUser(user);
        } else {
            // Not currently loaded — load, modify, and save in one step via the API
            // (works for offline players too, no console command needed).
            api.getUserManager().modifyUser(uuid, u -> applyGroupChange(u, oldGroup, newGroup));
        }
    }

    private void applyGroupChange(User user, String oldGroup, String newGroup) {
        if (oldGroup != null && !oldGroup.isBlank() && !oldGroup.equalsIgnoreCase(newGroup)) {
            user.data().remove(InheritanceNode.builder(oldGroup.toLowerCase()).build());
        }
        user.data().add(InheritanceNode.builder(newGroup.toLowerCase()).build());
    }
}
