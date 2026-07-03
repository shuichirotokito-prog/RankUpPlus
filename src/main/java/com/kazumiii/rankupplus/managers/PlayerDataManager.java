package com.kazumiii.rankupplus.managers;

import com.kazumiii.rankupplus.RankUpPlus;
import com.kazumiii.rankupplus.models.PlayerData;
import com.kazumiii.rankupplus.storage.DataStore;
import com.kazumiii.rankupplus.storage.MySQLDataStore;
import com.kazumiii.rankupplus.storage.YamlDataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final RankUpPlus plugin;
    private final DataStore dataStore;
    // ConcurrentHashMap, not HashMap: this cache is read from the async chat
    // renderer (ChatManager) and potentially from PlaceholderAPI requests that
    // some setups can trigger off the main thread, while joins/quits mutate it
    // on the main thread. A plain HashMap is not safe under that access pattern.
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(RankUpPlus plugin) {
        this.plugin = plugin;
        if (plugin.getConfigManager().getStorageType().equalsIgnoreCase("MYSQL")) {
            this.dataStore = new MySQLDataStore(plugin);
        } else {
            this.dataStore = new YamlDataStore(plugin);
        }
        this.dataStore.init();
    }

    public void reload() {
        saveAll();
        cache.clear();
        // Re-populate the cache for everyone currently online so getPlayerData()
        // doesn't start returning null for online players until they relog.
        for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerData data = dataStore.load(player.getUniqueId(), player.getName());
            cache.put(player.getUniqueId(), data);
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        return cache.get(uuid);
    }

    /**
     * Synchronous load-or-create. Used as a defensive fallback for code paths that
     * need data right now and can tolerate a (normally fast, YAML-backed) blocking
     * call. The primary join flow uses loadAsync() instead so a slow MySQL query
     * never blocks the main thread.
     */
    public PlayerData getOrCreate(UUID uuid, String name) {
        if (cache.containsKey(uuid)) {
            PlayerData data = cache.get(uuid);
            data.setName(name);
            return data;
        }
        PlayerData data = dataStore.load(uuid, name);
        cache.put(uuid, data);
        return data;
    }

    /**
     * Asynchronously loads a player's data (or returns the already-cached copy
     * immediately, wrapped in an already-completed future). Callers must hop back
     * onto the main thread via the Bukkit scheduler before touching Bukkit API
     * objects with the result.
     */
    public CompletableFuture<PlayerData> loadAsync(UUID uuid, String name) {
        PlayerData cached = cache.get(uuid);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        return dataStore.loadAsync(uuid, name);
    }

    /** Inserts an already-loaded PlayerData into the cache. Call only from the main thread. */
    public void cachePut(UUID uuid, PlayerData data) {
        cache.put(uuid, data);
    }

    public void save(PlayerData data) {
        dataStore.saveAsync(data);
    }

    public void saveAll() {
        dataStore.saveAllSync(new ArrayList<>(cache.values()));
    }

    public void unload(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) dataStore.saveAsync(data);
    }

    public Collection<PlayerData> getAll() {
        return cache.values();
    }

    /** Loads every known player's record. Can be slow — call this off the main thread. */
    public List<PlayerData> loadAllForLeaderboard() {
        return dataStore.loadAll();
    }

    /**
     * Loads a player's data for read-only use even if they are offline.
     * Checks the online cache first; falls back to reading their record from the
     * configured store without caching it. Returns a fresh default PlayerData if
     * no record exists yet.
     */
    public PlayerData getOfflineData(UUID uuid, String name) {
        PlayerData cached = cache.get(uuid);
        if (cached != null) return cached;
        return dataStore.load(uuid, name);
    }

    /**
     * Finds a player's data by their last-known stored name, online or offline,
     * without using the deprecated/blocking Bukkit.getOfflinePlayer(String) lookup.
     * Checks the online cache first, then queries the configured store.
     */
    public PlayerData findByName(String name) {
        for (PlayerData data : cache.values()) {
            if (name.equalsIgnoreCase(data.getName())) return data;
        }
        return dataStore.findByName(name);
    }

    public void close() {
        dataStore.close();
    }
}
